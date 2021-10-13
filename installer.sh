#!/usr/bin/env bash

if [ "$(uname)" == "Darwin" ]; then
  curl -O -L https://github.com/askonomm/shh/releases/latest/download/shh-macos && \
  mv shh-macos shh && \
  chmod +x shh
else
  curl -O -L https://github.com/askonomm/shh/releases/latest/download/shh-linux && \
  mv shh-linux shh && \
  chmod +x shh
fi

while [[ "$#" -gt 0 ]]; do
  case $1 in
    -g|--global) global="true"; shift ;;
  esac
  shift
done

if [ "$global" == "true" ]; then
  sudo mv shh /usr/local/bin/shh
fi