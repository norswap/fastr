package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public final class EnvironmentNodes {

    /**
     * Abstracted for use by other nodes that need to convert a list into an environment.
     */
    public static final class RList2EnvNode extends RBaseNode {
        private final boolean ignoreMissingNames;

        public RList2EnvNode() {
            this(false);
        }

        public RList2EnvNode(boolean ignoreMissingNames) {
            this.ignoreMissingNames = ignoreMissingNames;
        }

        @TruffleBoundary
        public REnvironment execute(RAbstractListVector list, REnvironment env) {
            if (list.getLength() == 0) {
                return env;
            }
            RStringVector names = list.getNames();
            if (names == null) {
                throw error(RError.Message.LIST_NAMES_SAME_LENGTH);
            }
            for (int i = list.getLength() - 1; i >= 0; i--) {
                String name = names.getDataAt(i);
                if (!ignoreMissingNames && name.length() == 0) {
                    throw error(RError.Message.ZERO_LENGTH_VARIABLE);
                }
                // in case of duplicates, last element in list wins
                if (env.get(name) == null) {
                    env.safePut(name, list.getDataAt(i));
                }
            }
            return env;
        }
    }

    public static final class GetFunctionEnvironmentNode extends RBaseNode {
        private final ConditionProfile noEnvProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile createProfile = ConditionProfile.createBinaryProfile();

        /**
         * Returns the environment that {@code func} was created in.
         */
        public Object getEnvironment(RFunction fun) {
            Frame enclosing = fun.getEnclosingFrame();
            if (noEnvProfile.profile(enclosing == null)) {
                return RNull.instance;
            }
            REnvironment env = RArguments.getEnvironment(enclosing);
            if (createProfile.profile(env == null)) {
                return REnvironment.createEnclosingEnvironments(enclosing.materialize());
            }
            return env;
        }

        public static GetFunctionEnvironmentNode create() {
            return new GetFunctionEnvironmentNode();
        }
    }
}
