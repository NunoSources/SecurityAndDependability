package common.dto;

import java.io.Serializable;

public class UserInfo implements Serializable {

	private static final long serialVersionUID = 2483374367095329560L;

	private String id;

	public UserInfo(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
