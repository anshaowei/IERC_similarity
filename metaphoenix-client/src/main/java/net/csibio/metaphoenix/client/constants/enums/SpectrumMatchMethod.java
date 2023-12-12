package net.csibio.metaphoenix.client.constants.enums;

public enum SpectrumMatchMethod {

    IonEntropyRankCosineSimilarity("IonEntropyRankCosineSimilarity"),
    Entropy("Entropy"),
    RankCosine("RankCosine"),
    Cosine("Cosine"),
    IonEntropyCosine("IonEntropyCosine"),
    IonEntropySpecEntropy("IonEntropySpecEntropy"),
    Unweighted_Entropy("Unweighted_Entropy"),
//    MetaPro("MetaPro"),
    Weighted_Cosine("Weighted_Cosine"),
    Euclidean("Euclidean"),
    Manhattan("Manhattan");

    private final String name;

    SpectrumMatchMethod(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

