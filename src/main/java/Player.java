public class Player{
    private String name;
    private String country;
    private double currentFormPoints;

    public Player(String name, String country){
        this.name = name;
        this.country = country;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public double getCurrentFormPoints() {
        return currentFormPoints;
    }

    public void setCurrentFormPoints(double currentFormPoints) {
        this.currentFormPoints = currentFormPoints;
    }
}
