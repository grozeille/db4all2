import sys
import json
import os

def main():
    try:
        # Read the hook payload from stdin
        input_data = sys.stdin.read()
        if not input_data.strip():
            # If no input, just allow
            print(json.dumps({"decision": "allow"}))
            return

        payload = json.loads(input_data)
        tool_call = payload.get("toolCall", {})
        tool_name = tool_call.get("name", "")
        args = tool_call.get("args", {})
        
        # We only care about run_command
        if tool_name != "run_command":
            print(json.dumps({"decision": "allow"}))
            return
            
        cmd = args.get("CommandLine", "").strip()
        is_daemon = args.get("IsDaemon", False)
        
        # Check if we should wrap this command with RTK
        # Do not wrap if it's a daemon/background process, or if it already uses rtk, or if it's empty
        if not cmd or is_daemon or cmd.startswith("rtk") or "rtk.exe" in cmd:
            print(json.dumps({"decision": "allow"}))
            return
            
        # We want to wrap git, mvn, npm, npx, kind, kubectl, and podman commands.
        # Check if the command starts with one of these keywords or contains them.
        keywords = ["git", "mvn", "npm", "npx", "kind", "kubectl", "podman"]
        should_wrap = False
        
        # Simple tokenization to check first word or command structure
        cmd_lower = cmd.lower()
        first_word = cmd_lower.split()[0] if cmd_lower.split() else ""
        
        # Strip potential dot-slash or path prefixes for checking
        first_word_clean = first_word.replace(".\\", "").replace("./", "").replace(".cmd", "").replace(".exe", "")
        
        if first_word_clean in keywords:
            should_wrap = True
            
        if should_wrap:
            # We assume rtk.exe is located in the .bin folder of the workspace.
            # The workspace path is available in payload["workspacePaths"][0].
            workspace_path = payload.get("workspacePaths", [""])[0]
            rtk_path = os.path.join(workspace_path, ".bin", "rtk.exe")
            
            # Double check if rtk.exe exists, if not we fall back to global 'rtk' or just the command
            if os.path.exists(rtk_path):
                new_cmd = f'& "{rtk_path}" {cmd}'
            else:
                new_cmd = f"& rtk {cmd}"
                
            print(json.dumps({
                "decision": "allow",
                "overwrite": {
                    "CommandLine": new_cmd
                }
            }))
        else:
            print(json.dumps({"decision": "allow"}))
            
    except Exception as e:
        # On error, log to stderr and fallback to allowing the original tool execution
        sys.stderr.write(f"Error in RTK wrapper: {str(e)}\n")
        print(json.dumps({"decision": "allow"}))

if __name__ == "__main__":
    main()
