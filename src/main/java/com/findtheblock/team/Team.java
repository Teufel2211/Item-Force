package com.findtheblock.team;

import java.util.Objects;

public class Team {
    public String id;
    public String name;

    public Team() {
    }

    public Team(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team team)) return false;
        return Objects.equals(id, team.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Team{id='" + id + "', name='" + name + "'}";
    }
}
