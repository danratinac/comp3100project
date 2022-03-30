# Using this Client
## Requirements
For the client to function properly:
1. The client must be running on Linux (tested specifically on Ubuntu Server for ARM, but other distros **should** work)
2. A copy of the **ds-sim** repository must be present on the Linux system the client is running on
## Running the Client
1. Compile the client class files by opening a terminal in the root folder of the repository and typing _"bash CompileClient.sh"_ (note: this will compile the utility classes as well)
2. Open a new terminal in the same folder as your **ds-server** and run **ds-server** according to the instructions in the **ds-server** user guide
3. In the first terminal, type _"java Client"_ to run the client
## Important Notes
- The file structure for the client must be kept intact; e.g. if you wish to move the client to a different folder, you must also take the entire utilities folder with it or the client will crash when run
- The **Server.java** file included in the repository is currently unused and contains demo code for a local Java server simulation that will **not** work with the current client; it will be removed at a later date if not required in Stage 2 of the project
