/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.serviceconfig.xml;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.duracloud.DeploymentOptionLocationType;
import org.duracloud.DeploymentOptionStateType;
import org.duracloud.DeploymentOptionType;
import org.duracloud.DeploymentOptionsType;
import org.duracloud.DeploymentStatusType;
import org.duracloud.DeploymentType;
import org.duracloud.DeploymentsType;
import org.duracloud.ModeSetType;
import org.duracloud.ModeType;
import org.duracloud.OptionInputType;
import org.duracloud.ServiceType;
import org.duracloud.SingleServiceType;
import org.duracloud.SystemConfigType;
import org.duracloud.SystemPropertyType;
import org.duracloud.UserConfigType;
import org.duracloud.UserPropertyType;
import org.duracloud.serviceconfig.Deployment;
import org.duracloud.serviceconfig.DeploymentOption;
import org.duracloud.serviceconfig.ServiceInfo;
import org.duracloud.serviceconfig.SystemConfig;
import org.duracloud.serviceconfig.user.MultiSelectUserConfig;
import org.duracloud.serviceconfig.user.Option;
import org.duracloud.serviceconfig.user.SelectableUserConfig;
import org.duracloud.serviceconfig.user.SingleSelectUserConfig;
import org.duracloud.serviceconfig.user.TextUserConfig;
import org.duracloud.serviceconfig.user.UserConfig;
import org.duracloud.serviceconfig.user.UserConfigMode;
import org.duracloud.serviceconfig.user.UserConfigModeSet;

/**
 * This class is responsible for serializing ServiceInfo objects into
 * service-config xml documents.
 *
 * @author Andrew Woods
 *         Date: Nov 17, 2009
 */
public class ServiceElementWriter {

    /**
     * This method serializes a single ServiceInfo object into a
     * single-service-config xml element.
     *
     * @param serviceInfo object to be serialized
     * @return xml service element with content from arg serviceInfo
     */
    public static SingleServiceType createSingleServiceElementFrom(ServiceInfo serviceInfo) {
        SingleServiceType singleServiceType = SingleServiceType.Factory
            .newInstance();
        populateElementFromObject(singleServiceType, serviceInfo);

        return singleServiceType;
    }

    /**
     * This method serializes a ServiceInfo object into a service-config xml
     * element that will be embedded in document containing mulitple such elements.
     *
     * @param serviceInfo object to serialize
     * @return xml service element with content from arg serviceInfo
     */
    public static ServiceType createElementFrom(ServiceInfo serviceInfo) {
        ServiceType serviceType = ServiceType.Factory.newInstance();
        populateElementFromObject(serviceType, serviceInfo);

        return serviceType;
    }

    private static void populateElementFromObject(ServiceType service,
                                                  ServiceInfo serviceInfo) {
        int id = serviceInfo.getId();
        if (id >= 0) {
            service.setId(id);
        }

        String contentId = serviceInfo.getContentId();
        if (!StringUtils.isBlank(contentId)) {
            service.setContentId(contentId);
        }

        String displayName = serviceInfo.getDisplayName();
        if (!StringUtils.isBlank(displayName)) {
            service.setDisplayName(displayName);
        }

        String serviceVersion = serviceInfo.getServiceVersion();
        if (!StringUtils.isBlank(serviceVersion)) {
            service.setServiceVersion(serviceVersion);
        }

        String userConfigVersion = serviceInfo.getUserConfigVersion();
        if (!StringUtils.isBlank(userConfigVersion)) {
            service.setUserConfigVersion(userConfigVersion);
        }

        String description = serviceInfo.getDescription();
        if (!StringUtils.isBlank(description)) {
            service.setDescription(description);
        }

        List<SystemConfig> systemConfigs = serviceInfo.getSystemConfigs();
        if (systemConfigs != null && systemConfigs.size() > 0) {
            SystemConfigType systemConfigType = service.addNewSystemConfig();
            populateSystemConfig(systemConfigType, systemConfigs);
        }


        List<UserConfigModeSet> userConfigModeSets = serviceInfo.getUserConfigModeSets();
        if (userConfigModeSets != null && userConfigModeSets.size() > 0) {
            UserConfigType  userConfigType = service.addNewUserConfig();
            populateUserConfigModeSets(userConfigType, userConfigModeSets);
        }

        List<DeploymentOption> deploymentOptions = serviceInfo.getDeploymentOptions();
        if (deploymentOptions != null && deploymentOptions.size() > 0) {
            DeploymentOptionsType deploymentOptionsType = service.addNewDeploymentOptions();
            int maxDeploymentsAllowed = serviceInfo.getMaxDeploymentsAllowed();
            if (maxDeploymentsAllowed >= -1) {
                deploymentOptionsType.setMax(maxDeploymentsAllowed);
            }

            populateDeploymentOptions(deploymentOptionsType, deploymentOptions);
        }

        List<Deployment> deployments = serviceInfo.getDeployments();
        if (deployments != null && deployments.size() > 0) {
            DeploymentsType deploymentsType = service.addNewDeployments();
            populateDeployments(deploymentsType, deployments);
        }

    }

    private static void populateUserConfigModeSets(UserConfigType userConfigType,
                                                   List<UserConfigModeSet> modeSets) {
        for (UserConfigModeSet modeSet : modeSets) {
            ModeSetType modeSetType = userConfigType.addNewModeSet();
            populateUserConfigModeSet(modeSetType, modeSet);
        }
    }

    private static void populateUserConfigModeSets(ModeType modeType,
                                                   List<UserConfigModeSet> modeSets) {
        for (UserConfigModeSet modeSet : modeSets) {
            ModeSetType modeSetType = modeType.addNewModeSet();
            populateUserConfigModeSet(modeSetType, modeSet);
        }
    }

    private static void populateUserConfigModeSet(ModeSetType modeSetType,
                                                  UserConfigModeSet modeSet) {
        int id = modeSet.getId();
        if (id != -1) {
            modeSetType.setId(id);
        }

        String name = modeSet.getName();
        if (!StringUtils.isBlank(name)) {
            modeSetType.setDisplayName(name);
        }

        String displayName = modeSet.getDisplayName();
        if (!StringUtils.isBlank(displayName)) {
            modeSetType.setDisplayName(displayName);
        }
        
        String value = modeSet.getValue();
        if (!StringUtils.isBlank(value)) {
            modeSetType.setValue(value);
        }

        List<UserConfigMode> modes = modeSet.getModes();
        if (modes != null && modes.size() > 0) {
            populateUserConfigModes(modeSetType, modes);
        }
    }

    private static void populateUserConfigModes(ModeSetType modeSetType,
                                                List<UserConfigMode> modes) {
        for (UserConfigMode mode : modes) {
            ModeType modeType = modeSetType.addNewMode();

            String name = mode.getName();
            if (null != name && !StringUtils.isBlank(name)) {
                modeType.setName(name);
            }

            String displayName = mode.getDisplayName();
            if (null != displayName && !StringUtils.isBlank(displayName)) {
                modeType.setDisplayName(displayName);
            }

            modeType.setSelected(mode.isSelected());

            List<UserConfig> userConfigs = mode.getUserConfigs();
            if (userConfigs != null && userConfigs.size() > 0) {
                populateUserConfigProperties(modeType, userConfigs);
            }

            List<UserConfigModeSet> modeSets = mode.getUserConfigModeSets();
            if (modeSets != null && modeSets.size() > 0) {
                populateUserConfigModeSets(modeType, modeSets);
            }
        }
    }

    private static void populateSystemConfig(SystemConfigType systemConfigType,
                                             List<SystemConfig> systemConfigs) {
        for (SystemConfig systemConfig : systemConfigs) {
            SystemPropertyType systemPropertyType = systemConfigType.addNewProperty();

            int id = systemConfig.getId();
            if (id >= 0) {
                systemPropertyType.setId(id);
            }

            String name = systemConfig.getName();
            if (!StringUtils.isBlank(name)) {
                systemPropertyType.setName(name);
            }

            String value = systemConfig.getValue();
            if (!StringUtils.isBlank(value)) {
                systemPropertyType.setValue(value);
            }

            String defaultValue = systemConfig.getDefaultValue();
            if (!StringUtils.isBlank(defaultValue)) {
                systemPropertyType.setDefaultValue(defaultValue);
            }
        }

    }

    private static void populateUserConfigProperties(UserConfigType userConfigType,
                                                     List<UserConfig> userConfigs) {
        for (UserConfig userConfig : userConfigs) {
            UserPropertyType userPropertyType = userConfigType.addNewProperty();
            populateUserConfigProperty(userConfig, userPropertyType);
        }
    }

    private static void populateUserConfigProperties(ModeType modeType,
                                                     List<UserConfig> userConfigs) {
        for (UserConfig userConfig : userConfigs) {
            UserPropertyType userPropertyType = modeType.addNewProperty();
            populateUserConfigProperty(userConfig, userPropertyType);
        }
    }

    private static void populateUserConfigProperty(UserConfig userConfig,
                                                   UserPropertyType userPropertyType) {
        int id = userConfig.getId();
        if (id >= 0) {
            userPropertyType.setId(id);
        }

        String name = userConfig.getName();
        if (!StringUtils.isBlank(name)) {
            userPropertyType.setName(name);
        }

        String displayName = userConfig.getDisplayName();
        if (!StringUtils.isBlank(displayName)) {
            userPropertyType.setDisplayName(displayName);
        }

        String exclusion = userConfig.getExclusion();
        if (!StringUtils.isBlank(exclusion)) {
            userPropertyType.setExclusion(exclusion);
        }

        UserConfig.InputType inputType = userConfig.getInputType();
        if (inputType != null) {
            if (inputType.equals(UserConfig.InputType.TEXT)) {
                userPropertyType.setInput(OptionInputType.TEXT);
                populateTextUserConfig(userPropertyType,
                                       (TextUserConfig) userConfig);
            } else if (inputType.equals(UserConfig.InputType.SINGLESELECT)) {
                userPropertyType.setInput(OptionInputType.SINGLESELECT);
                populateSelectableUserConfig(userPropertyType,
                                             (SingleSelectUserConfig) userConfig);
            } else if (inputType.equals(UserConfig.InputType.MULTISELECT)) {
                userPropertyType.setInput(OptionInputType.MULTISELECT);
                populateSelectableUserConfig(userPropertyType,
                                             (MultiSelectUserConfig) userConfig);

            } else {
                // FIXME: throw proper exception
                throw new RuntimeException("Unknown inputType: " + inputType);
            }
        }
    }

    private static void populateTextUserConfig(UserPropertyType userPropertyType,
                                               TextUserConfig userConfig) {
        String value = userConfig.getValue();
        if (!StringUtils.isBlank(value)) {
            userPropertyType.setValue(value);
        }

    }

    private static void populateSelectableUserConfig(UserPropertyType userPropertyType,
                                                     SelectableUserConfig userConfig) {
        List<Option> options = userConfig.getOptions();
        if (options != null && options.size() > 0) {
            for (Option option : options) {
                UserPropertyType.Option optionType = userPropertyType.addNewOption();

                String displayName = option.getDisplayName();
                if (!StringUtils.isBlank(displayName)) {
                    optionType.setDisplayName(displayName);
                }

                String value = option.getValue();
                if (!StringUtils.isBlank(value)) {
                    optionType.setValue(value);
                }

                optionType.setSelected(option.isSelected());
            }
        }

    }

    private static void populateDeploymentOptions(DeploymentOptionsType deploymentOptionsType,
                                                  List<DeploymentOption> deploymentOptions) {
        if (deploymentOptions != null && deploymentOptions.size() > 0) {
            for (DeploymentOption deploymentOption : deploymentOptions) {
                DeploymentOptionType deploymentOptionType = deploymentOptionsType
                    .addNewOption();

                String displayName = deploymentOption.getDisplayName();
                if (!StringUtils.isBlank(displayName)) {
                    deploymentOptionType.setDisplayName(displayName);
                }

                DeploymentOption.Location location = deploymentOption.getLocationType();
                if (location != null) {
                    if (location.equals(DeploymentOption.Location.PRIMARY)) {
                        deploymentOptionType.setLocation(
                            DeploymentOptionLocationType.PRIMARY);
                    } else if (location.equals(DeploymentOption.Location.NEW)) {
                        deploymentOptionType.setLocation(
                            DeploymentOptionLocationType.NEW);
                    } else if (location.equals(DeploymentOption.Location.EXISTING)) {
                        deploymentOptionType.setLocation(
                            DeploymentOptionLocationType.EXISTING);
                    } else {
                        // FIXME: throw proper exception
                        throw new RuntimeException(
                            "Unknown location: " + location);
                    }
                }

                String hostname = deploymentOption.getHostname();
                if (!StringUtils.isBlank(hostname)) {
                    deploymentOptionType.setHostname(hostname);
                }

                DeploymentOption.State state = deploymentOption.getState();
                if (state != null) {
                    if (state.equals(DeploymentOption.State.AVAILABLE)) {
                        deploymentOptionType.setState(DeploymentOptionStateType.AVAILABLE);
                    } else if (state.equals(DeploymentOption.State.UNAVAILABLE)) {
                        deploymentOptionType.setState(DeploymentOptionStateType.UNAVAILABLE);
                    } else {
                        // FIXME: throw proper exception
                        throw new RuntimeException("Unknown state: " + state);
                    }
                }
            }
        }

    }

    private static void populateDeployments(DeploymentsType deploymentsType,
                                            List<Deployment> deployments) {
        if (deployments != null && deployments.size() > 0) {
            for (Deployment deployment : deployments) {
                DeploymentType deploymentType = deploymentsType.addNewDeployment();

                int id = deployment.getId();
                if (id >= 0) {
                    deploymentType.setId(id);
                }

                String hostname = deployment.getHostname();
                if (!StringUtils.isBlank(hostname)) {
                    deploymentType.setHostname(hostname);
                }

                Deployment.Status status = deployment.getStatus();
                if (status != null) {
                    if (status.equals(Deployment.Status.STOPPED)) {
                        deploymentType.setStatus(DeploymentStatusType.STOPPED);
                    } else if (status.equals(Deployment.Status.STARTED)) {
                        deploymentType.setStatus(DeploymentStatusType.STARTED);
                    } else {
                        // FIXME: throw proper exception
                        throw new RuntimeException("Unknown status: " + status);
                    }
                }

                List<SystemConfig> systemConfigs = deployment.getSystemConfigs();
                if (systemConfigs != null && systemConfigs.size() > 0) {
                    SystemConfigType systemConfigType = deploymentType.addNewSystemConfig();
                    populateSystemConfig(systemConfigType, systemConfigs);
                }

                List<UserConfigModeSet> userConfigModeSets = deployment.getUserConfigModeSets();
                if (userConfigModeSets != null && userConfigModeSets.size() > 0) {
                    UserConfigType userConfigType = deploymentType.addNewUserConfig();
                    populateUserConfigModeSets(userConfigType, userConfigModeSets);
                }
            }
        }

    }

}
