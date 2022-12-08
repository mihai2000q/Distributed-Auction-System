import org.jgroups.JChannel;

public class GroupService implements IGroupService {
    public JChannel connect() {
        String channelName = System.getenv("GROUP") == null ? "DEFAULT_GROUP" : System.getenv("GROUP");
        try {
            JChannel channel = new JChannel();
            channel.connect(channelName);
            System.out.printf("✅    connected to jgroups channel: %s\n", channelName);
            channel.setDiscardOwnMessages(true);
            return channel;
        } catch (Exception e) {
            System.err.printf("🆘    could not connect to jgroups channel: %s\n", channelName);
        }

        return null;
    }
}
