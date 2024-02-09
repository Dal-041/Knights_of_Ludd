#!/bin/sh

### Edit
# Your mod name.
MOD_FOLDER_NAME="KnightsOfLudd"
echo "Folder name will be $MOD_FOLDER_NAME"
###


chmod +x ./zipMod.sh
sh ./zipMod.sh "./../.." "$MOD_FOLDER_NAME"