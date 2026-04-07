These patched AARs work around a Razorpay publishing issue that breaks AGP 9 namespace validation.

Sources:
- `standard-core:1.7.10`
- `core:1.0.10`

Patch:
- only the root `package="..."` in each bundled `AndroidManifest.xml` was changed
- classes, resources, and component names were left untouched

Why:
- upstream currently ships multiple Razorpay Android artifacts with the same manifest package, and AGP 9 rejects that during manifest merge
