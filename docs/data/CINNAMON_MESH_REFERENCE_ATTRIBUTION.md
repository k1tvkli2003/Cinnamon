# Cinnamon MeSH Reference Atlas

## Source

The reference atlas is derived from the 2026 production release of Medical
Subject Headings (MeSH), downloaded from:

<https://nlmpubs.nlm.nih.gov/projects/mesh/MESH_FILES/xmlmesh/desc2026.gz>

Source archive SHA-256:

`9fe35b3170652376a592daf69e91a80d6c693ecaf9c571ceb701d04204cb357d`

Accessed: 2026-07-23

**Courtesy of the U.S. National Library of Medicine.**

NLM does not endorse Cinnamon or any other product that uses this dataset.
The bundled reference reflects MeSH 2026 and may not include later NLM
updates. NLM terms and conditions:

<https://www.nlm.nih.gov/databases/download/terms_and_conditions.html>

## Selection policy

Cinnamon selects MeSH descriptors that have at least one tree number:

- in Anatomy (`A`), Diseases (`C`), Analytical/Diagnostic/Therapeutic
  Techniques and Equipment (`E`), Psychiatry and Psychology (`F`),
  Phenomena and Processes (`G`), or Health Care (`N`);
- at tree depth 3 or shallower; and
- with a non-empty NLM scope note.

The selection is a searchable reference layer. It does not replace Cinnamon's
authored medical-English learning entries, examples, IPA, usage notes,
collocations or editorial review process.

## Reproduction

```powershell
python scripts/build_mesh_reference.py
python scripts/verify_mesh_reference.py
```

The build script writes deterministic JSONL plus a manifest containing the
source archive hash, payload hash, selection policy and category counts.

