package org.gitbounty.gitbountybackend.service.codebase.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JGitCommitHistoryReaderTests {

	@Test
	void readCommits_returnsOldestFirst_whenOldIdIsZero() throws Exception {
		Path tmp = Files.createTempDirectory("jgit-history-test-");
		Path barePath = tmp.resolve("repo.git");
		Path workPath = tmp.resolve("work");

		// create bare
		try (Git ignored = Git.init().setBare(true).setDirectory(barePath.toFile()).call()) {
			assertNotNull(ignored);
		}

		RevCommit c1;
		RevCommit c2;
		try (Git work = Git.init().setDirectory(workPath.toFile()).setInitialBranch("main").call()) {
			StoredConfig config = work.getRepository().getConfig();
			config.setString("user", null, "name", "Tester");
			config.setString("user", null, "email", "test@example.com");
			config.save();

			Files.createDirectories(workPath);
			Files.writeString(workPath.resolve("file.txt"), "one");
			work.add().addFilepattern("file.txt").call();
			c1 = work.commit().setMessage("first").call();

			// no push required for local RevWalk testing

			Files.writeString(workPath.resolve("file.txt"), "two");
			work.add().addFilepattern("file.txt").call();
			c2 = work.commit().setMessage("second").call();
			JGitCommitHistoryReader reader = new JGitCommitHistoryReader();
			List<RevCommit> commits = reader.readCommits(work.getRepository(), org.eclipse.jgit.lib.ObjectId.fromString(c2.getName()), org.eclipse.jgit.lib.ObjectId.zeroId());

			// should include both commits and be ordered oldest-first
			assertEquals(2, commits.size());
			assertEquals(c1.getName(), commits.get(0).getName());
			assertEquals(c2.getName(), commits.get(1).getName());
		}
	}

	@Test
	void readCommits_excludesOldId_whenProvided() throws Exception {
		Path tmp = Files.createTempDirectory("jgit-history-test-");
		Path barePath = tmp.resolve("repo.git");
		Path workPath = tmp.resolve("work");

		try (Git ignored = Git.init().setBare(true).setDirectory(barePath.toFile()).call()) {
			assertNotNull(ignored);
		}

		RevCommit c1;
		RevCommit c2;
		try (Git work = Git.init().setDirectory(workPath.toFile()).setInitialBranch("main").call()) {
			StoredConfig config = work.getRepository().getConfig();
			config.setString("user", null, "name", "Tester");
			config.setString("user", null, "email", "test@example.com");
			config.save();

			Files.createDirectories(workPath);
			Files.writeString(workPath.resolve("file.txt"), "one");
			work.add().addFilepattern("file.txt").call();
			c1 = work.commit().setMessage("first").call();

			// no push required for local RevWalk testing

			Files.writeString(workPath.resolve("file.txt"), "two");
			work.add().addFilepattern("file.txt").call();
			c2 = work.commit().setMessage("second").call();

			JGitCommitHistoryReader reader = new JGitCommitHistoryReader();
			List<RevCommit> commits = reader.readCommits(work.getRepository(), org.eclipse.jgit.lib.ObjectId.fromString(c2.getName()), org.eclipse.jgit.lib.ObjectId.fromString(c1.getName()));

			// should include only the newer commit
			assertEquals(1, commits.size());
			assertEquals(c2.getName(), commits.get(0).getName());
		}
	}
}
