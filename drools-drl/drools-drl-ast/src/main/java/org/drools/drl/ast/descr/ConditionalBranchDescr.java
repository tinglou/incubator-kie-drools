/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.drl.ast.descr;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ConditionalBranchDescr extends BaseDescr {
    private EvalDescr condition = EvalDescr.TRUE;

    private NamedConsequenceDescr consequence;

    private ConditionalBranchDescr elseBranch;

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        super.readExternal( in );
        condition = (EvalDescr) in.readObject();
        consequence = (NamedConsequenceDescr) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject( condition );
        out.writeObject( consequence );
    }

    public EvalDescr getCondition() {
        return condition;
    }

    public void setCondition(EvalDescr condition) {
        this.condition = condition;
    }

    public NamedConsequenceDescr getConsequence() {
        return consequence;
    }

    public void setConsequence(NamedConsequenceDescr consequence) {
        this.consequence = consequence;
    }

    public ConditionalBranchDescr getElseBranch() {
        return elseBranch;
    }

    public void setElseBranch(ConditionalBranchDescr elseBranch) {
        this.elseBranch = elseBranch;
    }

    @Override
    public String toString() {
        return "if ( " + condition + " ) " + consequence + (elseBranch != null ? " else " + elseBranch : "");
    }

    @Override
    public void accept(DescrVisitor visitor) {
        visitor.visit(this);
    }

    public PatternDescr getReferringPatternDescr(AndDescr parent) {
        PatternDescr patternRelated = null;
        for (BaseDescr b : parent.getDescrs()) {
            if (b.equals(this)) {
                break;
            }
            if (b instanceof PatternDescr) {
                patternRelated = (PatternDescr) b; // keep the closest PatternDescr
            }
        }
        return patternRelated;
    }
}
