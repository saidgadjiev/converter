package ru.gadjini.telegram.converter.domain;

public class CreateOrUpdateResult {

    private TgUser user;

    private State state;

    public CreateOrUpdateResult(TgUser user, State state) {
        this.user = user;
        this.state = state;
    }

    public TgUser getUser() {
        return user;
    }

    public State getState() {
        return state;
    }

    public boolean isCreated() {
        return state == State.INSERTED;
    }

    public enum State {

        INSERTED("inserted"),

        UPDATED("updated");

        private final String desc;

        State(String desc) {
            this.desc = desc;
        }

        public static State fromDesc(String desc) {
            for (State state: values()) {
                if (state.desc.equals(desc)) {
                    return state;
                }
            }

            throw new IllegalArgumentException();
        }
    }
}
