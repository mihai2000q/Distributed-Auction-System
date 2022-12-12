import org.jgroups.Address;
import org.jgroups.View;

public class MembershipListener implements org.jgroups.MembershipListener {
    @Override
    public void viewAccepted(View view) {
        System.out.println("\nJgroups view changed\nnew view: " + view.toString() + "\n");
    }

    @Override
    public void suspect(Address address) {
        System.out.println("\nJgroups view suspected member crash: " + address.toString() + "\n");
    }

    @Override
    public void block() {
        System.out.println("\nJgroups view block indicator\n");
    }

    @Override
    public void unblock() {
        System.out.println("\nJgroups view unblock indicator\n");
    }
}
