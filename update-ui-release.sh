#!/bin/bash
git submodule init
git submodule update
cd saiku-ui
git checkout release
git pull origin release
