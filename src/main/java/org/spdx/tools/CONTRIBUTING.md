Contributing
============
Thank you for your interest in `Spdx-Java-Library`. The project is open-source software, and bug reports, suggestions, and most especially patches are welcome.

Issues
------
`Spdx-Java-Library` has a [project page on GitHub](https://github.com/spdx/Spdx-Java-Library) where you can [create an issue](https://github.com/spdx/Spdx-Java-Library/issues/new/choose) to report a bug, make a suggestion, or propose a substantial change or improvement that you might like to make. You may also wish to contact the SPDX working group technical team through its mailing list, [spdx-tech@lists.spdx.org](mailto:spdx-tech@lists.spdx.org).

If you would like to work on a fix for any issue, please assign the issue to yourself prior to creating a Pull Request.

Pull Requests
-------
The source code for `Spdx-Java-Library` is hosted on [github.com/spdx/Spdx-Java-Library](https://github.com/spdx/Spdx-Java-Library). Please review [open pull requests](https://github.com/spdx/Spdx-Java-Library/pulls) and [active branches](https://github.com/spdx/Spdx-Java-Library/branches) before committing time to a substantial revision. Work along similar lines may already be in progress.

To submit a pull request via GitHub, fork the repository, create a topic branch from `master` for your work, and send a pull request when ready. If you would prefer to send a patch or grant access to pull from your own Git repository, please contact the project's contributors by e-mail.

To contribute an implementation of a feature defined by a version of the SPDX specification later than the one supported by the current SPDX Tools release, clone the branch `spec/X.X`, where X.X is the major.minor version of the targeted specification (e.g. "3.0").

Once implemented, submit a pull request with `spec/X.X` branch as the parent branch.

Licensing
---------
However you choose to contribute, please sign-off in each of your commits that you license your contributions under the terms of [the Developer Certificate of Origin](https://developercertificate.org/). Git has utilities for signing off on commits: `git commit -s` signs a current commit, and `git rebase --signoff <revision-range>` retroactively signs a range of past commits.