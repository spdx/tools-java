# Release Checklist for the SPDX Java Tools

- [ ] Check for any warnings from the compiler and SpotBugs `mvn spotbugs:check`
- [ ] Run unit tests for all packages that depend on the application
- [ ] Run dependency check to find any potential vulnerabilities `mvn dependency-check:check`
- [ ] Update README.md to refer to the new version of the jar file, in the Syntax section and other sections
- [ ] Run `mvn release:prepare` - you will be prompted for the release - typically take the defaults
- [ ] Run `mvn release:perform`
- [ ] Release artifacts to Maven Central
- [ ] Create a Git release including release notes
- [ ] Zip up the files from the Maven archive and add them to the release
