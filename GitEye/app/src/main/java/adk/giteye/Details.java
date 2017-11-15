package adk.giteye;

/**
 * Created by Adu on 7/30/2017.
 */

public class Details {
    String name;
    String team;
    String points;

    public Details(String name, String team, String points) {
        this.name = name;
        this.team = team;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public String getTeam() {
        return team;
    }

    public String getPoints() {
        return points;
    }
}
