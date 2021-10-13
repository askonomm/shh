# Shh

A CLI password manager designed for efficiency.

## Install

### Locally

```shell
curl -s https://raw.githubusercontent.com/askonomm/shh/master/installer.sh | bash -s
```

You can then run babe as `./shh`, given that the Shh executable is in the current working directory.

### Globally

```shell
curl -s https://raw.githubusercontent.com/askonomm/shh/master/installer.sh | bash -s -- -g
```

You can then run shh as `shh` from anywhere.

## Usage

### Finding and creating passwords

To start, simply run `shh`. It will then prompt you for a name of a password, and if one isn't found, offers to create
one with that name. Whenever you create a new password, Shh will ask for a desired length of a password as well, which
is convenient for services that have strict rules on the length of password they accept.

To list the names of all existing passwords, run `shh list`.

**Note:** All passwords are be stored in a `.shh.edn` file in the user' home directory, unencrypted. This means that
your passwords will only be as safe as your computer. I might add some form of encryption later on (or you can make a
PR!), but for now it's not a priority for me.

### Changing passwords

To change an existing password, run `shh change {name}`.

### Deleting passwords

To delete a password, run `shh delete {name}`.

