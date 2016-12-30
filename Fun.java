/**
 * Created by bamibestelt on 2016-12-30.
 */
public class Fun {
    public String id;
    public FunType funType;
    public Fun (String id, FunType funType) {
        this.id = id;
        this.funType = funType;
    }
    public String toJVM() {
        return id + funType.toJVM();
    }
}