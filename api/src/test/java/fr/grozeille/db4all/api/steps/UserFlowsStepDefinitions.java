package fr.grozeille.db4all.api.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.grozeille.db4all.api.dto.AdminUpdatePasswordRequest;
import fr.grozeille.db4all.api.dto.CreateUserRequest;
import fr.grozeille.db4all.api.dto.DatasourceCreationRequest;
import fr.grozeille.db4all.api.dto.LocalFilesystemDatasourceConfigurationDto;
import fr.grozeille.db4all.api.dto.ProjectCreationRequest;
import fr.grozeille.db4all.api.dto.TableCreationRequest;
import fr.grozeille.db4all.api.model.DatasourceType;
import fr.grozeille.db4all.api.model.Project;
import fr.grozeille.db4all.api.model.TableSourceKind;
import fr.grozeille.db4all.api.model.User;
import fr.grozeille.db4all.api.repository.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CucumberContextConfiguration
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
    private PasswordEncoder passwordEncoder;

    // --- State managed between steps ---
    private String currentUser;
    private String currentUserRoles;
    private String createdProjectId;
    private String createdDatasourceId;
    private ResultActions lastResult;

    @Given("the system is set up with an initial super admin {string}")
    public void setupInitialAdmin(String email) {
        if (!userRepository.existsById(email)) {
            User admin = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode("ABC123abc"))
                    .superAdmin(true)
                    .build();
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
        CreateUserRequest request = CreateUserRequest.builder()
                .email(email)
                .password(password)
                .build();

        lastResult = mockMvc.perform(post("/api/v2/users")
                .with(user(currentUser).roles(currentUserRoles))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Then("the user {string} is created successfully")
    public void theUserIsCreatedSuccessfully(String email) throws Exception {
        lastResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @When("I, as an admin, change the password for {string} to {string}")
    public void iChangeThePassword(String email, String newPassword) throws Exception {
        AdminUpdatePasswordRequest request = AdminUpdatePasswordRequest.builder()
            .password(newPassword)
                .build();

        lastResult = mockMvc.perform(put("/api/v2/users/" + email + "/password")
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
        ProjectCreationRequest request = ProjectCreationRequest.builder()
                .name(projectName)
                .description("A test project")
                .build();

        lastResult = mockMvc.perform(post("/api/v2/projects")
                .with(user(currentUser).roles(currentUserRoles))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        
        String response = lastResult.andReturn().getResponse().getContentAsString();
        this.createdProjectId = objectMapper.readValue(response, Project.class).getId();
    }

    @Then("the project {string} is created successfully")
    public void theProjectIsCreatedSuccessfully(String projectName) throws Exception {
        lastResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(projectName));
    }

        @When("I create a local datasource named {string} for this project")
        public void iCreateALocalDatasourceForThisProject(String datasourceName) throws Exception {
        Path rootPath = Files.createTempDirectory("db4all-datasource-");

        DatasourceCreationRequest request = DatasourceCreationRequest.builder()
            .name(datasourceName)
            .description("A local datasource for tests")
            .type(DatasourceType.LOCAL_FILESYSTEM)
            .readOnly(true)
            .configuration(LocalFilesystemDatasourceConfigurationDto.builder()
                .rootPath(rootPath.toString())
                .build())
            .build();

        lastResult = mockMvc.perform(post("/api/v2/projects/" + createdProjectId + "/datasources")
            .with(user(currentUser).roles(currentUserRoles))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));

        String response = lastResult.andReturn().getResponse().getContentAsString();
        this.createdDatasourceId = objectMapper.readTree(response).path("id").asText();
        }

        @Then("the datasource {string} is created successfully")
        public void theDatasourceIsCreatedSuccessfully(String datasourceName) throws Exception {
        lastResult.andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(datasourceName));
        }

    @Then("I see {int} project when I list all projects")
    public void iSeeProjectsWhenIListAllProjects(int count) throws Exception {
        mockMvc.perform(get("/api/v2/projects?page=0&size=10")
                .with(user(currentUser).roles(currentUserRoles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(count));
    }

    @When("I create a new table named {string} for this project")
    public void iCreateANewTable(String tableName) throws Exception {
        ObjectNode configuration = objectMapper.createObjectNode();
        configuration.put("path", "test.csv");
        configuration.put("separator", ",");
        configuration.put("firstRowAsHeader", true);

        TableCreationRequest request = TableCreationRequest.builder()
                .name(tableName)
                .description("A test table")
            .datasourceId(createdDatasourceId)
            .sourceKind(TableSourceKind.CSV)
            .configuration(configuration)
                .build();

        lastResult = mockMvc.perform(post("/api/v2/projects/" + createdProjectId + "/tables")
                .with(user(currentUser).roles(currentUserRoles))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Then("the table {string} is created successfully")
    public void theTableIsCreatedSuccessfully(String tableName) throws Exception {
        lastResult.andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(tableName));
    }

    @Then("I see {int} table when I list all tables for this project")
    public void iSeeTablesWhenIListAllTables(int count) throws Exception {
        mockMvc.perform(get("/api/v2/projects/" + createdProjectId + "/tables?page=0&size=10")
                .with(user(currentUser).roles(currentUserRoles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(count));
    }
}
