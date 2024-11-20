#!/usr/bin/env bash

openssl genrsa -out private-key-rsa.pem 4096
openssl rsa -pubout -in private-key-rsa.pem -out public-key.pem

# Convert private key to PKCS#8 format
openssl pkcs8 -topk8 -nocrypt -inform pem -in private-key-rsa.pem -outform pem -out private-key.pem
