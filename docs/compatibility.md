# Compile target and older-server loading

- **Compile and test dependency:** Paper API `26.3.build.+`.
- **Recommended/gameplay-tested server:** Paper/Purpur 26.3.
- **Loader API floor:** 1.21.11 in `plugin.yml`.
- **Java runtime:** 25 on all server versions.

The loader declaration is independent of the API used to build the plugin.
There is no older compile dependency, alternate build profile, or separate
older-version JAR. CI builds once against 26.3 and downloads that exact artifact
into isolated Paper 1.21.11, 26.1.2, 26.2 and 26.3 startup jobs. Each job checks
enablement, server readiness, the plugin help command and clean shutdown.

Older versions are permitted to load when their APIs are compatible. Startup
checks do not prove every gameplay path; combat, shields, farming, anvil handling
and persistence still need in-game testing on older versions. Future use of
26.3-only APIs may require capability checks or fallbacks for those paths.
Unknown material names are already skipped by the item-profile loader.

Versions below 1.21.11 are outside the current compatibility floor. Purpur follows
Paper API compatibility; automated startup checks here run on Paper only.
