package com.dianping.puma.server.builder.impl;

import com.dianping.puma.core.codec.JsonEventCodec;
import com.dianping.puma.core.constant.Status;
import com.dianping.puma.core.entity.PumaTask;
import com.dianping.puma.core.entity.SrcDBInstance;
import com.dianping.puma.core.holder.BinlogInfoHolder;
import com.dianping.puma.core.model.BinlogStat;
import com.dianping.puma.core.monitor.NotifyService;
import com.dianping.puma.core.service.SrcDBInstanceService;
import com.dianping.puma.datahandler.DefaultDataHandler;
import com.dianping.puma.datahandler.DefaultTableMetaInfoFetcher;
import com.dianping.puma.parser.DefaultBinlogParser;
import com.dianping.puma.parser.Parser;
import com.dianping.puma.sender.FileDumpSender;
import com.dianping.puma.sender.Sender;
import com.dianping.puma.sender.dispatcher.SimpleDispatcherImpl;
import com.dianping.puma.server.DefaultTaskExecutor;
import com.dianping.puma.server.TaskExecutor;
import com.dianping.puma.server.builder.TaskExecutorBuilder;
import com.dianping.puma.storage.DefaultArchiveStrategy;
import com.dianping.puma.storage.DefaultCleanupStrategy;
import com.dianping.puma.storage.DefaultEventStorage;
import com.dianping.puma.storage.LocalFileBucketIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service("taskExecutorBuilder")
public class DefaultTaskExecutorBuilder implements TaskExecutorBuilder {

	@Autowired
	SrcDBInstanceService srcDBInstanceService;

	@Autowired
	BinlogInfoHolder binlogInfoHolder;

	@Autowired
	NotifyService notifyService;

	@Autowired
	private JsonEventCodec jsonCodec;

	@Value("fileSender-")
	String fileSenderName;

	@Value("storage-")
	String storageName;

	@Value("dispatch-")
	String dispatchName;

	@Value("/data/appdatas/puma/storage/master/")
	String masterStorageBaseDir;

	@Value("Bucket-")
	String masterBucketFilePrefix;

	@Value("1000")
	int maxMasterBucketLengthMB;

	@Value("50")
	int maxMasterFileCount;

	@Value("/data/appdatas/puma/storage/slave/")
	String slaveStorageBaseDir;

	@Value("Bucket-")
	String slaveBucketFilePrefix;

	@Value("1000")
	int maxSlaveBucketLengthMB;

	@Value("50")
	int maxSlaveFileCount;

	@Value("/data/appdatas/puma/binlogIndex/")
	String binlogIndexBaseDir;

	private static final Logger LOG = LoggerFactory.getLogger(DefaultTaskExecutorBuilder.class);

	public TaskExecutor build(PumaTask pumaTask) throws Exception {
		
		try {
			DefaultTaskExecutor taskExecutor = new DefaultTaskExecutor();

			// Base.
			String taskId = pumaTask.getId();
			String taskName = pumaTask.getName();
			taskExecutor.setTaskId(taskId);
			taskExecutor.setTaskName(taskName);
			taskExecutor.setNotifyService(notifyService);
			//taskExecutor.setServerId(taskId.hashCode());

			// Bin log.
			taskExecutor.setBinlogInfoHolder(binlogInfoHolder);
			taskExecutor.setBinlogInfo(pumaTask.getBinlogInfo());
			taskExecutor.setBinlogStat(new BinlogStat());

			// Source database.
			String srcDBInstanceId = pumaTask.getSrcDBInstanceId();
			SrcDBInstance srcDBInstance = srcDBInstanceService.find(srcDBInstanceId);
			taskExecutor.setDbServerId(srcDBInstance.getServerId());
			taskExecutor.setDBHost(srcDBInstance.getHost());
			taskExecutor.setPort(srcDBInstance.getPort());
			taskExecutor.setDBUsername(srcDBInstance.getUsername());
			taskExecutor.setDBPassword(srcDBInstance.getPassword());

			// Parser.
			Parser parser = new DefaultBinlogParser();
			parser.start();
			taskExecutor.setParser(parser);

			// Handler.
			DefaultDataHandler dataHandler = new DefaultDataHandler();
			dataHandler.setNotifyService(notifyService);
			DefaultTableMetaInfoFetcher tableMetaInfo = new DefaultTableMetaInfoFetcher();
			tableMetaInfo.setMetaDBHost(srcDBInstance.getMetaHost());
			tableMetaInfo.setMetaDBPort(srcDBInstance.getMetaPort());
			tableMetaInfo.setMetaDBUsername(srcDBInstance.getUsername());
			tableMetaInfo.setMetaDBPassword(srcDBInstance.getPassword());
			dataHandler.setTableMetasInfoFetcher(tableMetaInfo);
			dataHandler.start();
			taskExecutor.setDataHandler(dataHandler);

			// File sender.
			List<Sender> senders = new ArrayList<Sender>();
			FileDumpSender sender = new FileDumpSender();
			sender.setName(fileSenderName + taskName);
			sender.setNotifyService(notifyService);

			// File sender storage.
			DefaultEventStorage storage = new DefaultEventStorage();
			storage.setName(storageName + taskName);
			storage.setCodec(jsonCodec);

			// File sender master storage.
			LocalFileBucketIndex masterBucketIndex = new LocalFileBucketIndex();
			masterBucketIndex.setBaseDir(masterStorageBaseDir + taskName);
			masterBucketIndex.setBucketFilePrefix(masterBucketFilePrefix);
			masterBucketIndex.setMaxBucketLengthMB(maxMasterBucketLengthMB);
			masterBucketIndex.start();
			storage.setMasterBucketIndex(masterBucketIndex);

			// File sender slave storage.
			LocalFileBucketIndex slaveBucketIndex = new LocalFileBucketIndex();
			slaveBucketIndex.setBaseDir(slaveStorageBaseDir + taskName);
			slaveBucketIndex.setBucketFilePrefix(slaveBucketFilePrefix);
			slaveBucketIndex.setMaxBucketLengthMB(maxSlaveBucketLengthMB);
			slaveBucketIndex.start();
			storage.setSlaveBucketIndex(slaveBucketIndex);

			// Archive strategy.
			DefaultArchiveStrategy archiveStrategy = new DefaultArchiveStrategy();
			archiveStrategy.setServerName(taskName);
			archiveStrategy.setMaxMasterFileCount(maxMasterFileCount);
			storage.setArchiveStrategy(archiveStrategy);

			// Clean up strategy.
			DefaultCleanupStrategy cleanupStrategy = new DefaultCleanupStrategy();
			cleanupStrategy.setPreservedDay(pumaTask.getPreservedDay());
			storage.setCleanupStrategy(cleanupStrategy);

			storage.setBinlogIndexBaseDir(binlogIndexBaseDir + taskName);
			storage.start();
			sender.setStorage(storage);
			sender.start();
			senders.add(sender);

			// Dispatch.
			SimpleDispatcherImpl dispatcher = new SimpleDispatcherImpl();
			dispatcher.setName(dispatchName + taskName);
			dispatcher.setSenders(senders);
			dispatcher.start();
			taskExecutor.setDispatcher(dispatcher);

			// Set puma task status.
			taskExecutor.setStatus(Status.WAITING);

			return taskExecutor;
		} catch (Exception e) {
			LOG.error("Build puma task `{}` error: {}.", pumaTask.getName(), e.getMessage());
			throw e;
		}
	}
}