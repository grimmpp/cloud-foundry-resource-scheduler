package grimmpp.AppManager.model.cfClient;

import lombok.Data;

import java.util.Date;

@Data
public class LastOperation {
    private String type;
    private String state;
    private String description;
    private Date updated_at;
    private Date created_at;
}
