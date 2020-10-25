package jenkins.plugin.android.emulator.sdk.pipeline;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.android_emulator.Constants;
import jenkins.plugin.android.emulator.AndroidSDKConstants;
import jenkins.plugin.android.emulator.AndroidSDKUtil;
import jenkins.plugin.android.emulator.sdk.home.HomeLocator;
import jenkins.plugin.android.emulator.tools.AndroidSDKInstallation;

abstract class AbstractCLIStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private final String emulatorTool;
    private final HomeLocator homeLocationStrategy;

    protected AbstractCLIStepExecution(String emulatorTool, HomeLocator homeLocationStrategy, StepContext context) {
        super(context);
        this.emulatorTool = emulatorTool;
        this.homeLocationStrategy = homeLocationStrategy;
    }

    @Override
    protected Void run() throws Exception {
        FilePath workspace = getContext().get(FilePath.class);
        workspace.mkdirs();

        AndroidSDKInstallation sdk = AndroidSDKUtil.getAndroidSDK(emulatorTool);
        if (sdk == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.noInstallationFound(emulatorTool));
        }

        Computer computer = workspace.toComputer();
        if (computer == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.nodeNotAvailable());
        }
        Node node = computer.getNode();
        if (node == null) {
            throw new AbortException(jenkins.plugin.android.emulator.Messages.nodeNotAvailable());
        }

        TaskListener listener = getContext().get(TaskListener.class);
        EnvVars env = getContext().get(EnvVars.class);
        sdk = sdk.forNode(node, listener);
        sdk = sdk.forEnvironment(env);

        sdk.buildEnvVars(env);

        // configure home location
        FilePath homeLocation = homeLocationStrategy.locate(workspace);
        if (homeLocation != null) {
            env.put(Constants.ENV_VAR_ANDROID_SDK_HOME, homeLocation.getRemote());

            FilePath emulatorLocation = homeLocation.child(AndroidSDKConstants.ANDROID_CACHE);
            env.put(AndroidSDKConstants.ENV_ANDROID_EMULATOR_HOME, emulatorLocation.getRemote());

            FilePath avdPath = emulatorLocation.child("avd");
            avdPath.mkdirs(); // ensure that this folder exists
            env.put(AndroidSDKConstants.ENV_ANDROID_AVD_HOME, avdPath.getRemote());
        }

        return doRun(sdk, listener, env);
    }

    protected abstract Void doRun(AndroidSDKInstallation sdk, TaskListener listener, EnvVars env) throws Exception;
}
