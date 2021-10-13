# Shh

A CLI password manager designed for efficiency.

## Install

### Locally

```shell
curl -s https://raw.githubusercontent.com/askonomm/shh/master/installer.sh | bash -s
```

You can then run babe as `./shh` or `./shh watch`, given that the Babe executable is in the current working directory.

### Globally

```shell
curl -s https://raw.githubusercontent.com/askonomm/shh/master/installer.sh | bash -s -- -g
```

You can then run shh as `shh` or `shh watch` from anywhere.

## Usage

### Finding and creating passwords

To start, simply run `shh`. It will then prompt you for a name of a password, and if one isn't found, offers to create
one with that name.

To list the names of all existing passwords, run `shh list`.

### Changing passwords

To change an existing password, run `shh change {name}`.

### Deleting passwords

To delete a password, run `shh delete {name}`.

