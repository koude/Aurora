// Valid ELF wrapper so Android's ABI split and native-library extraction select
// only the patch for the installed architecture. The payload is appended later.
__attribute__((visibility("default"))) int aurora_root_patch_container(void) { return 1; }
