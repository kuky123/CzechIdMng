package eu.bcvsolutions.idm.rpt.ldap;

import java.io.File;
import java.io.IOException;
import java.io.NotActiveException;

import org.apache.commons.lang3.StringUtils;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.springframework.util.Assert;

/**
 * Ldap server for testing
 *
 * @author Ondrej Kopr <kopr@xyxy.cz>
 *
 */
public class LdapServer {

	/*
	 * Default administration password and username 
	 */
	public static final String ADMIN_USERNAME = "uid=admin,ou=system";
	public static final String ADMIN_PASSWORD = "secret";

	/*
	 * Basic root context and port
	 */
	public static final String DEFAULT_OU = "ou=Users";
	public static final String DEFAULT_ROOT = "dc=example,dc=com";
	public static final int DEFAULT_PORT = 11369;

	/*
	 * Existing group for testing merge
	 */
	public static final String GROUP_A_DN = "cn=GroupA,ou=groups,ou=system";
	public static final String GROUP_B_DN = "cn=GroupB,ou=groups,ou=system";
	public static final String GROUP_C_DN = "cn=GroupC,ou=groups,ou=system";

	/*
	 * Existing user on system
	 */
	public static final String TEST_USER_DN = "uid=TestUser,ou=users,ou=system";
	public static final String TEST_USER_PASSWORD = "testuser";
	public static final String TEST_USER_CN = "Test User";

	/*
	 * User defined ou, root and port
	 */
	private final String ou;
	private final String root;
	private final Integer port;

	/*
	 * I know singleton :)
	 */
	private LdapService server = null;
	private DirectoryService directory = null; // now exists only one directory
	private File tempDb = null;
	private boolean initFirstData = true; // init data only for first instance, second run throw exception

	/**
	 * Create default server
	 */
	public LdapServer() {
		ou = null;
		root = null;
		port = null;
	}

	/**
	 * Create server with given default ou root and port
	 *
	 * @param ou
	 * @param root
	 * @param port
	 */
	public LdapServer(String ou, String root, int port) {
		this.ou = ou;
		this.root = root;
		this.port = port;
	}

	/**
	 * Run server
	 *
	 * @throws Exception
	 */
	public void run() throws Exception {
		if (existsAndIsStarted()) {
			return;
		}
		// Create default partion
		Partition partition = createPartition();

		// Create temp working directory
		tempDb = createTempDbStructure();

		// Create directory, now is only one
		directory = createDirectory(partition);

		// Create server
		server = createServer(directory);
		
		// And internal init
		internalStart();
		
		if (initFirstData) {
			// InitData after start
			initFirstData();
			initFirstData = false;
		}
	}

	/**
	 * Stop server
	 *
	 * @throws Exception
	 */
	public void stop() throws Exception {
		if (existsAndIsStarted()) {
			directory.shutdown();
			server.stop();
			tempDb.delete();
		}
	}

	/**
	 * Check if exists given dn
	 *
	 * @param dn
	 * @return
	 * @throws Exception
	 */
	public boolean existsEntityByDn(String dn) throws Exception {
		return getAdminSession().exists(new LdapDN(dn));
	}

	/**
	 * Get {@link ServerEntry} by dn
	 *
	 * @param dn
	 * @return
	 * @throws Exception
	 */
	public ServerEntry getEntityByDn(String dn) throws Exception {
		return getAdminSession().lookup(new LdapDN(dn));
	}

	/**
	 * Delete given dn
	 *
	 * @param dn
	 * @throws Exception
	 */
	public void deleteEntityByDn(String dn) throws Exception {
		getAdminSession().delete(new LdapDN(dn));
	}

	/**
	 * Add given {@link ServerEntry}
	 * 
	 * @param entity
	 * @throws Exception
	 */
	public void addEntity(ServerEntry entity) throws Exception {
		getAdminSession().add(entity);
	}

	/**
	 * Get admin session and check if server is started, otherwise throw exeception
	 *
	 * @return
	 * @throws Exception
	 */
	private CoreSession getAdminSession() throws Exception {
		if (existsAndIsStarted()) {
			return directory.getAdminSession();
		}
		throw new NotActiveException("Server is not running.");
	}

	/**
	 * Exists and run server
	 *
	 * @return
	 */
	private boolean existsAndIsStarted() {
		return server != null && server.isStarted();
	}

	/**
	 * Internal start
	 *
	 * @throws Exception
	 */
	private void internalStart() throws Exception {
		directory.startup();
		server.start();
	}

	/**
	 * Create {@link Partition}
	 *
	 * @return
	 * @throws LdapInvalidDnException
	 */
	private Partition createPartition() {
		// Create default partion
		Partition partition = new JdbmPartition();
		if (StringUtils.isEmpty(this.ou)) {
			partition.setId(DEFAULT_OU);
		} else {
			partition.setId(this.ou);
		}

		if (StringUtils.isEmpty(this.root)) {
			partition.setSuffix(DEFAULT_ROOT);
		} else {
			partition.setId(this.root);
		}
		return partition;
	}

	/**
	 * Create temp working directory with delete on exit
	 *
	 * @return
	 * @throws IOException
	 */
	private File createTempDbStructure() throws IOException {
		// Create temp working directory
		File temp = new File(System.getProperty("java.io.tmpdir"));
//		temp.deleteOnExit();
		
		temp.mkdirs();
		
		return temp;
	}

	/**
	 * Create {@link DirectoryService}
	 *
	 * @param partition
	 * @return
	 * @throws Exception
	 */
	private DirectoryService createDirectory(Partition partition) throws Exception {
		DirectoryService directory = new DefaultDirectoryService();
		directory.addPartition(partition);
		directory.setShutdownHookEnabled(false);
		directory.setWorkingDirectory(tempDb);
		return directory;
	}

	/**
	 * Create {@link LdapServer}
	 *
	 * @param directory
	 * @return
	 */
	private LdapService createServer(DirectoryService directory) {
		LdapService server = new LdapService();
		server.setSocketAcceptor(new SocketAcceptor(null));
		server.setDirectoryService(directory);
		if (this.port != null) {
			server.setIpPort(this.port);
		} else {
			server.setIpPort(DEFAULT_PORT);
		}
		return server;
	}

	/**
	 * Initialization first data. MemberOf and some groups.
	 *
	 * @throws Exception
	 */
	private void initFirstData() throws Exception {
		File initFile = new File(getClass().getClassLoader().getResource("eu/bcvsolutions/idm/ldap/ldap-init.ldif").getFile());
		LdifFileLoader ldif = new LdifFileLoader(directory.getAdminSession(), initFile.getPath());
        ldif.execute();

        // Check created groups
		boolean exists = existsEntityByDn(GROUP_A_DN);
		Assert.isTrue(exists);

		exists = existsEntityByDn(GROUP_B_DN);
		Assert.isTrue(exists);

		exists = existsEntityByDn(GROUP_C_DN);
		Assert.isTrue(exists);

		// Check created user
		exists = existsEntityByDn(TEST_USER_DN);
		Assert.isTrue(exists);
	}
	
	/**
	 * Generate uid with default ou and default root including other ou and username.
	 * Result will be for example:
	 * uid=username,ou=users,ou=system
	 *
	 * @param username
	 * @param otherOu
	 * @return
	 */
	public static String generateDn(String username, String ... otherOu) {
		StringBuilder result = new StringBuilder();
		result.append("uid=");
		result.append(username);
		result.append(",");
		
		if (otherOu != null) {
			for (String ou : otherOu) {
				result.append("ou=");
				result.append(ou);
				result.append(",");
			}
		}
		result.append("ou=users,ou=system");

		return result.toString();
	}
}
