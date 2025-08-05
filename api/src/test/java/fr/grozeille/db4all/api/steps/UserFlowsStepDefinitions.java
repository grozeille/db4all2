package fr.grozeille.db4all.api.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.grozeille.db4all.api.dto.AdminUpdatePasswordRequest;
import fr.grozeille.db4all.api.dto.CreateUserRequest;
import fr.grozeille.db4all.api.model.Project;
import fr.grozeille.db4all.api.model.Table;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.repository.ProjectRepository;
import fr.grozeille.db4all.api.repository.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class UserFlowsStepDefinitions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProjectRepository projectRepository;

    // --- State managed between steps ---
    private String currentUser;
    private String currentUserRoles;
    private String createdProjectId;
    private ResultActions lastResult;

    @Given("the system is set up with an initial super admin {string}")
    public void setupInitialAdmin(String email) {
        if (!userRepository.existsById(email)) {
            User admin = new User();
            admin.setEmail(email);
            admin.setPasswordHash("encoded-password"); // Corrected to setPasswordHash
            admin.setSuperAdmin(true);
            userRepository.save(admin);
        }
    }

    @Given("I am logged in as the super admin {string}")
    public void iAmLoggedInAsSuperAdmin(String email) {
        this.currentUser = email;
        this.currentUserRoles = "SUPER_ADMIN";
    }
    
    @Given("I am logged in as the new user {string}")
    public void iAmLoggedInAsNewUser(String email) {
        this.currentUser = email;
        this.currentUserRoles = "USER";
    }

    @When("I create a new user with email {string} and password {string}")
    public void iCreateANewUser(String email, String password) throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(email); 
        request.setPassword(password);

        lastResult = mockMvc.perform(post("/api/users")
                .with(user(currentUser).roles(currentUserRoles))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Then("the user {string} is created successfully")
    public void theUserIsCreatedSuccessfully(String email) throws Exception {
        lastResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email)); // Corrected to check 'email'
    }

    @When("I, as an admin, change the password for {string} to {string}")
    public void iChangeThePassword(String email, String newPassword) throws Exception {
        AdminUpdatePasswordRequest request = new AdminUpdatePasswordRequest();
        request.setNewPassword(newPassword);

        lastResult = mockMvc.perform(put("/api/users/" + email + "/password") // Corrected to use email in URL
                .with(user(currentUser).roles(currentUserRoles))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Then("the password change is successful")
    public void thePasswordChangeIsSuccessful() throws Exception {
        lastResult.andExpect(status().isNoContent());
    }

    @When("I create a new project named {string}")
    public void iCreateANewProject(String projectName) throws Exception {
        Project newProject = new Project();
        newProject.setName(projectName);

        lastResult = mockMvc.perform(post("/api/projects")
                .with(user(currentUser).roles(currentUserRoles))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProject)));
        
        String response = lastResult.andReturn().getResponse().getContentAsString();
        this.createdProjectId = objectMapper.readValue(response, Project.class).getId();
    }

    @Then("the project {string} is created successfully")
    public void theProjectIsCreatedSuccessfully(String projectName) throws Exception {
        lastResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(projectName));
    }

    @Then("I see {int} project when I list all projects")
    public void iSeeProjectsWhenIListAllProjects(int count) throws Exception {
        mockMvc.perform(get("/api/projects?page=0&size=10")
                .with(user(currentUser).roles(currentUserRoles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(count));
    }

    @When("I create a new table named {string} for this project")
    public void iCreateANewTable(String tableName) throws Exception {
        Table newTable = new Table();
        newTable.setName(tableName);

        lastResult = mockMvc.perform(post("/api/projects/" + createdProjectId + "/tables")
                .with(user(currentUser).roles(currentUserRoles))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTable)));
    }

    @Then("the table {string} is created successfully")
    public void theTableIsCreatedSuccessfully(String tableName) throws Exception {
        lastResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(tableName));
    }

    @Then("I see {int} table when I list all tables for this project")
    public void iSeeTablesWhenIListAllTables(int count) throws Exception {
        mockMvc.perform(get("/api/projects/" + createdProjectId + "/tables?page=0&size=10")
                .with(user(currentUser).roles(currentUserRoles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(count));
    }
}
