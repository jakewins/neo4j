#!/bin/bash

rm -rf public/webadmin-html
node_modules/.bin/brunch build -m
