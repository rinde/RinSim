#!/bin/bash

# http://www.steveklabnik.com/automatically_update_github_pages_with_travis_example/

rev=$(git rev-parse --short HEAD)


# TODO built website here

cd stage/_site

git init
git config user.name "Rinde van Lon"
git config user.email "rindevanlon@gmail.com"

git remote add upstream "https://$GH_TOKEN@github.com/rust-lang/rust-by-example.git"
git fetch upstream && git reset upstream/gh-pages



touch .

git add -A .
git commit -m "rebuild pages at ${rev}"
git push -q upstream HEAD:gh-pages