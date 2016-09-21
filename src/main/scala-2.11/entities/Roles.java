package entities;

/**
 * Created by ivan on 21.09.16.
 */
public enum Roles {
    ADMIN(0),
    USER(1);
    private final int roleId;
    Roles(int i){
        roleId = i;
    }

    public int getRoleId() {
        return roleId;
    }
}
