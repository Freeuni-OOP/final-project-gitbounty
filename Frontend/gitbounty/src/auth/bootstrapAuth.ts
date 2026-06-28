import { KeycloakAdapter } from "./KeycloakAdapter";
import { setGlobalAuth, notifyAuthChanged } from "./authInstance";

const authAdapter = new KeycloakAdapter(notifyAuthChanged);

setGlobalAuth(authAdapter);

authAdapter.initialize();

export default authAdapter;