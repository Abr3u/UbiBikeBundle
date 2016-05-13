package pt.ulisboa.tecnico.cmov.ubibike;

enum SharedPreferenceKey {
    NAME,
    USERNAME,
    PASSWORD;

    public String toString() {
        return Integer.toString(this.ordinal());
    }
}