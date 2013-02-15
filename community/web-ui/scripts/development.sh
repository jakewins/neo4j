#!/bin/bash

rm -rf target
mkdir -p target/classes
node_modules/.bin/brunch watch
