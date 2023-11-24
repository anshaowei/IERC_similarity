package net.csibio.metaphoenix.client.constants.enums;

public enum SpectrumMatchMethod {

    Cosine("Cosine"),
    Entropy("Entropy"),
    IonEntropyCosine("IonEntropyCosine"),
    IonEntropySpecEntropy("IonEntropySpecEntropy"),
    Unweighted_Entropy("Unweighted_Entropy"),
    MetaPro("MetaPro"),
    Weighted_Cosine("Weighted_Cosine"),
    Euclidean("Euclidean"),
    Manhattan("Manhattan"),
    RankCosine("RankCosine"),
    IonEntropyRankCosineSimilarity("IonEntropyRankCosineSimilarity");

    private final String name;

    SpectrumMatchMethod(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

