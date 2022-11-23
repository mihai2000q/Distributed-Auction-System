import java.io.Serializable;

public record UserDto(String username, String password, Constants.ClientType clientType) implements Serializable {
}
