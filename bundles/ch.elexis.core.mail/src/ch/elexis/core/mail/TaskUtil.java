package ch.elexis.core.mail;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.LoggerFactory;

import ch.elexis.core.mail.internal.SendMailRunnable;
import ch.elexis.core.model.tasks.IIdentifiedRunnable;
import ch.elexis.core.model.tasks.TaskException;
import ch.elexis.core.tasks.model.ITask;
import ch.elexis.core.tasks.model.ITaskDescriptor;
import ch.elexis.core.tasks.model.ITaskService;
import ch.elexis.core.tasks.model.TaskTriggerType;

@Component
public class TaskUtil {
	
	private static ITaskService taskService;
	
	@Reference
	public void setTaskService(ITaskService taskService){
		TaskUtil.taskService = taskService;
	}
	
	/**
	 * Create a new {@link ITaskDescriptor} for sending the message using the account.
	 * 
	 * @param accountId
	 * @param message
	 * @return
	 */
	public static Optional<ITaskDescriptor> createSendMailTaskDescriptor(String accountId,
		MailMessage message){
		
		Optional<IIdentifiedRunnable> sendMailRunnable = taskService.getIdentifiedRunnables()
			.stream().filter(ir -> ir.getId().equals(SendMailRunnable.RUNNABLE_ID)).findFirst();
		
		if (sendMailRunnable.isPresent()) {
			try {
				ITaskDescriptor descriptor =
					taskService.createTaskDescriptor(sendMailRunnable.get());
				Map<String, Serializable> runContext = descriptor.getRunContext();
				runContext.put("accountId", accountId);
				runContext.put("message", message);
				descriptor.setRunContext(runContext);
				taskService.saveTaskDescriptor(descriptor);
				return Optional.of(descriptor);
			} catch (TaskException e) {
				LoggerFactory.getLogger(TaskUtil.class)
					.error("Error creating send mail task descriptor", e);
			}
		}
		return Optional.empty();
	}
	
	public static ITask executeTaskSync(ITaskDescriptor iTaskDescriptor,
		IProgressMonitor progressMonitor) throws TaskException{
		ITask task = taskService.trigger(iTaskDescriptor, progressMonitor, TaskTriggerType.MANUAL,
			Collections.emptyMap());
		while (!task.isFinished()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		return task;
	}
}