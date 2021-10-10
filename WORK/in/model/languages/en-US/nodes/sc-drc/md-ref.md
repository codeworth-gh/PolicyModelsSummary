### Do the metadata include qualified references to other (meta)data?

How well specified are the relationships included in the metadata, e.g., applied in the context of publications, does the resource use the DataCite or some other schema/standard that specifies the relationship of an identifier to the data set, e.g., a PubMed ID for a publication that first reported the data set.  Should be machine friendly, e.g., ID’s for publications rather than free text.

#### Options

* **best:** The relationship between the data set or element and an identifier that references an external entity is clearly specified, e.g., the people listed and the related publication are clearly specified.
   * Data publication:  DOI or PMID
   * Author:            ORCID + metadata
   * Contact person:    ORCID + appropriate metadata
* **good:** Identifiers provided but no explicit relationships given
  * Publication:  Tagged but doesn’t specify the relationship of the publication to the data set clearly
  * Creators:     Tagged but doesn’t specify key roles clearly
* **worst:** Authors and publication are provided in free text
