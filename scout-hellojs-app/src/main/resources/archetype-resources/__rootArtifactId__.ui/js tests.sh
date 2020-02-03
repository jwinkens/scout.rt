#!/bin/sh

# This script starts the testserver and executes all JavaScript tests. It expects that npm install has already been executed previously.
#
# To make this script work you need a current version of Node.js (>=12.1.0) and npm (>=6.9.0).
# Node.js (incl. npm) is available here: https://nodejs.org/.

# Abort the script if any command fails
set -e

# Specify the path to the node and npm binaries
PATH=$PATH:/usr/local/bin

# Check if npm is available
command -v npm >/dev/null 2>&1 || { echo >&2 "npm cannot be found. Make sure Node.js is installed and the PATH variable correctly set. See the content of this script for details."; exit 1; }

# Execute the tests
echo "Running 'npm testserver:start'"
npm run testserver:start
echo "npm testserver:start finished successfully!"
