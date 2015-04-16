
# Release how to
 
 1. commit final change to develop branch
 1. check
   * 
        ```
        mvn verify
        ```
   * 
        ```
        mvn versions:set -DnewVersion=X.Y.Z
        ```
   * verify and update release notes
   * verify backwards compatibility version is correct
   * update version number in README (remove -SNAPSHOT)
 1. commit
 1. switch to master and merge
   1. 
        ```
        git checkout master
        ```
    1. 
        ```
        git pull
        ```
   1. 
        ```
        git merge develop
        ```
 1. deploy
   1. 
        ```
        mvn clean deploy -DperformRelease=true
        ``` 
        Possibly need to add:
        ```
        -Dgpg.executable=gpg2
        ```
   1. Wait for success confirmation by email ("Nexus: Promotion Completed").
 1. 
      ```
      git push
      ```
 1. goto github.com, draft a new release from master:
    * use vX.Y.Z as tag name and release title
    * use text from releasenotes.md as description (don't copy the title since it is already added by GitHub)
 1. go back to develop, prepare next version
   1. 
        ```
        git checkout develop
        ```
   1. 
        ```
        mvn versions:set -DnewVersion=X.Y.Z-SNAPSHOT
        ```
   1. update backwards compatibility version (if needed)
   1. update version in README (add -SNAPSHOT)
 1. commit
 1. make the code [citable](https://guides.github.com/activities/citable-code/)

