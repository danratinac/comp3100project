# Using this Client
## Requirements
For the client to function properly, the following must be true:
1. The client is running on Linux (tested specifically on Ubuntu Server for ARM)
2. A copy of the ds-sim repository is present on the Linux system the client is running on
## Running the Client
1. Compile the client class files by opening a terminal in the root folder of the repository and typing _"bash CompileClient.sh"_ (note: this will compile the utility classes as well)
2. Open a new terminal in the same folder as your _ds-server_ and run _ds-server_ according to the instructions in the accompanying documentation
3. In the first terminal, type _"java Client"_ to run the client
## Important Notes
- The file structure for the client must be kept intact; e.g. if you wish to move the client to a different folder, you must also take the entire utilities folder with it or the client will crash when run
