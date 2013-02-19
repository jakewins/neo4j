#!/bin/bash

#!/bin/bash

SERVER_LOG="./target/server.log"
BREW_LOG="./target/brew.log"
SERVER_PID=0
BRUNCH_PID=0
TESTRUNNER_PID=0

function startServer() 
{
  echo "Compiling and booting server, saving output in '$SERVER_LOG'.."
  
  mvn antrun:run 1> $SERVER_LOG 2> $SERVER_LOG 0> $SERVER_LOG &
  SERVER_PID=$!

  lastLine=""
  expected='0.0.0:7474'
  bad='Failed to start Neo Server on port'
  until [[ "$lastLine" == *"$expected"* ]]; do

    lastLine=$(tail -n 1 $SERVER_LOG)
    
    if [[ "$lastLine" == *"$bad"* ]]; then
      echo "Starting server failed, please see $SERVER_LOG"
      exit 1
    fi
  
    sleep 1
  done

  echo "Server started."
}

function autoCompileWebadmin() 
{
  echo "Booting brunch autocompiler.."

  node_modules/.bin/brunch watch &
  BRUNCH_PID=$!
}

function startTestRunner() 
{
  echo "Launching test runner.."

  scripts/test.sh &
  TESTRUNNER_PID=$!
}

function infiniteSleep() 
{
  while(true); do
    sleep 1;
  done;
}

function onExit()
{
  echo "Stopping test runner.."
  kill $TESTRUNNER_PID
  
  echo "Stopping brunch compiler.."
  kill $BRUNCH_PID

  echo "Stopping the server.."
  kill $SERVER_PID
}

# 
# Main
#

trap onExit EXIT

mkdir target

startServer;

rm -rf ./public/webadmin-html/*
autoCompileWebadmin;
startTestRunner;
infiniteSleep;

