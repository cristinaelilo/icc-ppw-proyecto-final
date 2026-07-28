package ec.edu.ups.icc.proyecto.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private Rule login = new Rule(5, 60);
    private Rule register = new Rule(3, 3600);
    private Rule aPublic = new Rule(60, 60);
    private Rule authenticated = new Rule(120, 60);
    private Rule reports = new Rule(5, 60);
    private Block block = new Block(5, 900);

    public Rule getLogin() { return login; }
    public void setLogin(Rule login) { this.login = login; }
    public Rule getRegister() { return register; }
    public void setRegister(Rule register) { this.register = register; }
    public Rule getPublic() { return aPublic; }
    public void setPublic(Rule aPublic) { this.aPublic = aPublic; }
    public Rule getAuthenticated() { return authenticated; }
    public void setAuthenticated(Rule authenticated) { this.authenticated = authenticated; }
    public Rule getReports() { return reports; }
    public void setReports(Rule reports) { this.reports = reports; }
    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }

    public static class Rule {
        private long limit;
        private long windowSeconds;
        public Rule() {}
        public Rule(long limit, long windowSeconds) { this.limit = limit; this.windowSeconds = windowSeconds; }
        public long getLimit() { return limit; }
        public void setLimit(long limit) { this.limit = limit; }
        public long getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(long windowSeconds) { this.windowSeconds = windowSeconds; }
    }

    public static class Block {
        private int maxFailedAttempts;
        private long blockDurationSeconds;
        public Block() {}
        public Block(int maxFailedAttempts, long blockDurationSeconds) {
            this.maxFailedAttempts = maxFailedAttempts;
            this.blockDurationSeconds = blockDurationSeconds;
        }
        public int getMaxFailedAttempts() { return maxFailedAttempts; }
        public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }
        public long getBlockDurationSeconds() { return blockDurationSeconds; }
        public void setBlockDurationSeconds(long blockDurationSeconds) { this.blockDurationSeconds = blockDurationSeconds; }
    }
}
