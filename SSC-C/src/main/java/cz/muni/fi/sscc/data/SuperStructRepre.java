package cz.muni.fi.sscc.data;

import java.util.List;
import java.util.Objects;

public record SuperStructRepre(String name, List<SSMember> member) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;

        SuperStructRepre that = (SuperStructRepre) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
