---
SPDX-FileType: DOCUMENTATION
SPDX-License-Identifier: CC-BY-4.0
---

Contributing
============

Thank you for your interest in `tools-java`. The project is open-source software, and bug reports, suggestions, and most especially patches are welcome.

All contributions must include a "Signed-off-by" line in the commit message.

This indicates that the contribution is made pursuant to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/), a copy of which is included below.

Issues
------

`tools-java` has a [project page on GitHub](https://github.com/spdx/tools-java/) where you can [create an issue](https://github.com/spdx/tools-java/issues/new) to report a bug, make a suggestion, or propose a substantial change or improvement that you might like to make. You may also wish to contact the SPDX working group technical team through its mailing list, [spdx-tech@lists.spdx.org](mailto:spdx-tech@lists.spdx.org).

If you would like to work on a fix for any issue, please assign the issue to yourself prior to creating a patch.

Patches
-------

The source code for `spdx-tools` is hosted on [github.com/spdx/tools-java](https://github.com/spdx/tools-java). Please review [open pull requests](https://github.com/spdx/tools-java/pulls) and [active branches](https://github.com/spdx/tools-java/branches) before committing time to a substantial revision. Work along similar lines may already be in progress.

To submit a patch via GitHub, fork the repository, create a topic branch from `master` for your work, and send a pull request when ready. If you would prefer to send a patch or grant access to pull from your own Git repository, please contact the project's contributors by e-mail.

To contribute an implementation of a feature defined by a version of the SPDX specification later than the one supported by the current SPDX Tools release, clone the branch `spec/X.X`, where X.X is the major.minor version of the targeted specification (e.g. "3.0").

Once implemented, submit a pull request with `spec/X.X` branch as the parent branch.

Licensing
---------

New **code files** should include a [short-form SPDX ID](https://spdx.org/ids) at the top, indicating the project license for code, which is Apache-2.0. This should look like the following:

```java
// SPDX-License-Identifier: Apache-2.0
```

Developer Certificate of Origin (DCO)
-------------------------------------

```text
Developer Certificate of Origin
Version 1.1

Copyright (C) 2004, 2006 The Linux Foundation and its contributors.
1 Letterman Drive
Suite D4700
San Francisco, CA, 94129

Everyone is permitted to copy and distribute verbatim copies of this
license document, but changing it is not allowed.


Developer's Certificate of Origin 1.1

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
    have the right to submit it under the open source license
    indicated in the file; or

(b) The contribution is based upon previous work that, to the best
    of my knowledge, is covered under an appropriate open source
    license and I have the right under that license to submit that
    work with modifications, whether created in whole or in part
    by me, under the same open source license (unless I am
    permitted to submit under a different license), as indicated
    in the file; or

(c) The contribution was provided directly to me by some other
    person who certified (a), (b) or (c) and I have not modified
    it.

(d) I understand and agree that this project and the contribution
    are public and that a record of the contribution (including all
    personal information I submit with it, including my sign-off) is
    maintained indefinitely and may be redistributed consistent with
    this project or the open source license(s) involved.
```
