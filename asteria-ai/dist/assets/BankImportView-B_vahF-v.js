import{G as P,O as B,W as i,k as $,aj as w,L as g,P as t,$ as y,a0 as o,_ as u,u as r,A as j,M as I,c as k,F as q,aF as W}from"./vendor-BtDxEGf_.js";import{E as x}from"./element-plus-BeMEpVc5.js";import{G as z}from"./GlassCard-_TE1lAtK.js";import{S as v}from"./SoftButton-YyMviYl9.js";import{S as Z,R as J}from"./SoftProgress-DFmbNZyq.js";import{B as K}from"./BreathingLoader-CNEbDHjU.js";import{c as M,_ as L}from"./index-DCRCg31D.js";import{i as Q,g as Y}from"./bank-B9Z7phBA.js";import{u as ee}from"./bank-QxqCjo2n.js";import{a as te}from"./format-CUCz8HEC.js";import{C as le}from"./check-Dl3qXyZP.js";import{A as se}from"./arrow-right-mTY8eK3e.js";import{T as oe}from"./triangle-alert-A31kmnXn.js";/**
 * @license lucide-vue-next v1.0.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const ae=M("clipboard-copy",[["rect",{width:"8",height:"4",x:"8",y:"2",rx:"1",ry:"1",key:"tgr4d6"}],["path",{d:"M8 4H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2",key:"4jdomd"}],["path",{d:"M16 4h2a2 2 0 0 1 2 2v4",key:"3hqy98"}],["path",{d:"M21 14H11",key:"1bme5i"}],["path",{d:"m15 10-4 4 4 4",key:"5dvupr"}]]);/**
 * @license lucide-vue-next v1.0.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const E=M("cloud-upload",[["path",{d:"M12 13v8",key:"1l5pq0"}],["path",{d:"M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242",key:"1pljnt"}],["path",{d:"m8 17 4-4 4 4",key:"1quai1"}]]);/**
 * @license lucide-vue-next v1.0.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const ne=M("file-text",[["path",{d:"M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z",key:"1oefj6"}],["path",{d:"M14 2v5a1 1 0 0 0 1 1h5",key:"wfsgrz"}],["path",{d:"M10 9H8",key:"b1mrlr"}],["path",{d:"M16 13H8",key:"t4e002"}],["path",{d:"M16 17H8",key:"z1uh3a"}]]);/**
 * @license lucide-vue-next v1.0.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const ie=M("info",[["circle",{cx:"12",cy:"12",r:"10",key:"1mglay"}],["path",{d:"M12 16v-4",key:"1dtifu"}],["path",{d:"M12 8h.01",key:"e9boi3"}]]),ue={class:"guide"},re={class:"guide-block"},de={class:"block-head"},pe=`【第 1 题】题型：单选题
题目：网页是由 HTML 语言来实现的，HTML 语言是
选项：
  A. 大型数据库
  B. 网页源文件中出现的唯一一种语言
  C. 网络通信协议
  D. 超文本标记语言
正确答案：D`,N=`请按下面的标准格式整理题目文本。

【重要】只调整结构和补充标签：不得改动任何原文文字，不得编造或补全答案，不得增删题目。

标准格式：
【第 1 题】题型：单选题
题目：题干内容
选项：
  A. 选项内容
  B. 选项内容
  C. 选项内容
  D. 选项内容
正确答案：A

格式要求：
1. 每道题以【第 N 题】开头，N 从 1 开始递增
2. 题型只能是这五种之一：单选题、多选题、判断题、填空题、简答题
3. 选项行固定写成「字母. 内容」；判断题也要写成 A. 正确 / B. 错误
4. 答案写法：
   - 单选题：单个字母，如 A
   - 多选题：字母连写、不分隔、升序，如 ACD
   - 判断题：A 表示正确，B 表示错误
   - 填空题：多个空用中文分号「；」分隔
   - 简答题：答案原文
5. 原文没给答案的，「正确答案：」后面留空，不要自己编
6. 只输出整理后的纯文本，不要任何说明文字，不要 markdown 代码块
7. 如果支持文件输出，请把整理结果保存成一个 txt 文件（文件名：整理后的题目.txt）给我下载；
   不支持文件输出就直接输出纯文本，我自行保存成 txt

待整理文本：
（把题目粘贴在这里）`,ce=P({__name:"FormatGuideDialog",props:{modelValue:{type:Boolean}},emits:["update:modelValue"],setup(T,{emit:_}){const S=T,V=_,c=$({get:()=>S.modelValue,set:a=>V("update:modelValue",a)});async function d(){try{await navigator.clipboard.writeText(N),x.success("提示词已复制，粘贴到 AI 对话框即可");return}catch{}const a=document.createElement("textarea");a.value=N,a.style.position="fixed",a.style.opacity="0",document.body.appendChild(a),a.select();const l=document.execCommand("copy");document.body.removeChild(a),l?x.success("提示词已复制，粘贴到 AI 对话框即可"):x.warning("复制失败，请手动选中提示词复制")}return(a,l)=>{const b=w("el-dialog");return g(),B(b,{modelValue:c.value,"onUpdate:modelValue":l[1]||(l[1]=f=>c.value=f),title:"题目格式要求",width:"600px","append-to-body":""},{footer:i(()=>[o(v,{variant:"ghost",onClick:l[0]||(l[0]=f=>c.value=!1)},{default:i(()=>[...l[8]||(l[8]=[u("知道了",-1)])]),_:1})]),default:i(()=>[t("div",ue,[l[6]||(l[6]=t("p",{class:"guide-intro"}," 按下面的标准格式整理，识别率最高。文件是从网页或 Word 里复制来的、格式比较乱时， 建议先交给 AI 整理一次再上传。 ",-1)),t("div",{class:"guide-block"},[l[2]||(l[2]=t("span",{class:"block-title"},"标准格式示例",-1)),t("pre",{class:"code-block"},y(pe))]),t("div",re,[t("div",de,[l[4]||(l[4]=t("span",{class:"block-title"},"用 AI 快速整理",-1)),o(v,{variant:"outline",icon:r(ae),onClick:d},{default:i(()=>[...l[3]||(l[3]=[u(" 复制提示词 ",-1)])]),_:1},8,["icon"])]),l[5]||(l[5]=t("p",{class:"block-desc"}," 复制后粘贴到任意 AI 对话窗口，把题目接在最后面；把 AI 的回复保存成 txt 再上传即可。 ",-1))]),l[7]||(l[7]=t("div",{class:"guide-block"},[t("span",{class:"block-title"},"注意"),t("ul",{class:"tips"},[t("li",null,"题型只能是这五种：单选题、多选题、判断题、填空题、简答题"),t("li",null,[u("判断题答案写 "),t("b",null,"A"),u("（正确）或 "),t("b",null,"B"),u("（错误）")]),t("li",null,[u("多选题答案字母连写、不分隔、升序，例如 "),t("b",null,"ACD")]),t("li",null,"原文没给答案就留空，不要自己编"),t("li",null,"没有「【第 N 题】」题号时题目会被整批跳过，务必保留")])],-1))])]),_:1},8,["modelValue"])}}}),me=L(ce,[["__scopeId","data-v-3a55a751"]]),ve={class:"bank-import-view"},fe={class:"format-tip"},ke={class:"form-rows"},ge={class:"form-row"},ye={class:"form-row"},be={style:{display:"flex","align-items":"center",gap:"10px"}},Ce={class:"form-actions"},xe={class:"progress-head"},_e={class:"progress-file"},Ae={class:"file-name"},Ie={class:"file-size"},we={class:"stage-line"},Me={key:1,class:"result-block success"},Se={class:"result-figure success"},Ve={class:"text-muted"},he={class:"result-actions"},Be={key:2,class:"result-block failed"},Te={class:"result-figure failed"},Fe={class:"error-msg"},De={class:"result-actions"},ze=20*1024*1024,Ee=P({__name:"BankImportView",setup(T){const _=W(),S=ee(),V=["docx","pdf","txt"],c=k(),d=k(null),a=k(""),l=k(!1),b=k(!1),f=k(!1),n=k(null);let C=null;j(h);function F(s){var p;const e=((p=s.name.split(".").pop())==null?void 0:p.toLowerCase())??"";return V.includes(e)?s.size>ze?(x.error("文件大小不能超过 20MB"),!1):!0:(x.error("仅支持 DOCX / PDF / TXT 文件"),!1)}function G(s,e){var p,A;if(!F(s.raw)){(p=c.value)==null||p.clearFiles(),d.value=null;return}e.length>1&&((A=c.value)==null||A.clearFiles()),d.value=s.raw,a.value||(a.value=s.name.replace(/\.[^.]+$/,""))}function R(s){var p;(p=c.value)==null||p.clearFiles();const e=s[0];F(e)&&(d.value=e,a.value||(a.value=e.name.replace(/\.[^.]+$/,"")))}async function U(){if(!(!d.value||l.value)){l.value=!0;try{const s=await Q({file:d.value,bankName:a.value||void 0,aiParse:b.value});n.value={taskId:s.taskId,status:s.status,fileName:d.value.name,fileSize:d.value.size,progress:0,bankId:null,totalCount:0,errorMessage:null},H()}catch{}finally{l.value=!1}}}function H(){h();const s=async()=>{if(n.value){try{const e=await Y(n.value.taskId);if(n.value=e,e.status==="SUCCESS"||e.status==="FAILED"){S.fetchBanks({},!0);return}}catch{}C=setTimeout(s,1500)}};C=setTimeout(s,1500)}function h(){C&&(clearTimeout(C),C=null)}const O=$(()=>{var s;switch((s=n.value)==null?void 0:s.status){case"PENDING":return"排队等待解析…";case"PARSING":return"解析文档 → 识别题目…";case"AI_FORMATTING":return"AI 整理格式中（文件排版较乱，正转成标准格式）…";case"AI_PROCESSING":return"AI 解析入库中…";default:return"处理中…"}});function D(){var s;h(),n.value=null,d.value=null,a.value="",(s=c.value)==null||s.clearFiles()}return(s,e)=>{const p=w("el-upload"),A=w("el-input"),X=w("el-switch");return g(),I("div",ve,[n.value?(g(),B(z,{key:1,class:"progress-card"},{default:i(()=>[t("div",xe,[o(r(ne),{size:20,"stroke-width":1.6}),t("div",_e,[t("span",Ae,y(n.value.fileName),1),t("span",Ie,y(r(te)(n.value.fileSize)),1)])]),n.value.status!=="SUCCESS"&&n.value.status!=="FAILED"?(g(),I(q,{key:0},[o(Z,{value:n.value.progress,"show-text":"",class:"progress-bar"},null,8,["value"]),t("div",we,[o(K,{small:""}),t("span",null,y(O.value),1)])],64)):n.value.status==="SUCCESS"?(g(),I("div",Me,[t("div",Se,[o(r(le),{size:30,"stroke-width":2})]),e[17]||(e[17]=t("h3",null,"导入完成",-1)),t("p",Ve,"共识别 "+y(n.value.totalCount)+" 道题目，已整理入库",1),t("div",he,[o(v,{variant:"outline",onClick:D},{default:i(()=>[...e[15]||(e[15]=[u("继续导入",-1)])]),_:1}),o(v,{icon:r(se),onClick:e[4]||(e[4]=m=>r(_).push(`/banks/${n.value.bankId}`))},{default:i(()=>[...e[16]||(e[16]=[u("查看题库",-1)])]),_:1},8,["icon"])])])):(g(),I("div",Be,[t("div",Te,[o(r(oe),{size:30,"stroke-width":1.6})]),e[20]||(e[20]=t("h3",null,"导入失败",-1)),t("p",Fe,y(n.value.errorMessage||"文件解析失败，请检查文件内容"),1),e[21]||(e[21]=t("p",{class:"text-muted"},"数据已回滚，不会产生脏数据",-1)),t("div",De,[o(v,{variant:"outline",icon:r(J),onClick:D},{default:i(()=>[...e[18]||(e[18]=[u("重新上传",-1)])]),_:1},8,["icon"]),o(v,{variant:"ghost",onClick:e[5]||(e[5]=m=>f.value=!0)},{default:i(()=>[...e[19]||(e[19]=[u("查看格式要求",-1)])]),_:1})])]))]),_:1})):(g(),B(z,{key:0,title:"导入题库",class:"import-card"},{default:i(()=>[o(p,{ref_key:"uploadRef",ref:c,drag:"","auto-upload":!1,limit:1,accept:".docx,.pdf,.txt","on-change":G,"on-exceed":R,class:"upload-area"},{default:i(()=>[o(r(E),{size:42,"stroke-width":1.4,class:"upload-icon"}),e[7]||(e[7]=t("div",{class:"el-upload__text"},[u("拖拽文件到此处，或 "),t("em",null,"点击选择")],-1)),e[8]||(e[8]=t("div",{class:"upload-hint"},"支持 DOCX / PDF / TXT，单个文件 ≤ 20MB",-1))]),_:1},512),t("div",fe,[o(r(ie),{size:14,"stroke-width":1.8}),e[9]||(e[9]=t("span",null,"格式比较乱、怕识别不出来？",-1)),t("button",{type:"button",class:"link-btn",onClick:e[0]||(e[0]=m=>f.value=!0)}," 查看格式要求 / 复制 AI 整理提示词 ")]),t("div",ke,[t("div",ge,[e[10]||(e[10]=t("label",{class:"form-label"},"题库名称（可选，默认取文件名）",-1)),o(A,{modelValue:a.value,"onUpdate:modelValue":e[1]||(e[1]=m=>a.value=m),placeholder:"例如：数据库期末复习题库"},null,8,["modelValue"])]),t("div",ye,[e[12]||(e[12]=t("label",{class:"form-label"},"AI 解析",-1)),t("div",be,[o(X,{modelValue:b.value,"onUpdate:modelValue":e[2]||(e[2]=m=>b.value=m)},null,8,["modelValue"]),e[11]||(e[11]=t("span",{class:"text-muted",style:{"font-size":"13px"}}," 导入后用 AI 逐题生成解析；没有答案的题会自动补答案（较慢，100 题约 3~8 分钟） ",-1))])])]),t("div",Ce,[o(v,{variant:"ghost",onClick:e[3]||(e[3]=m=>r(_).push("/banks"))},{default:i(()=>[...e[13]||(e[13]=[u("返回",-1)])]),_:1}),o(v,{icon:r(E),loading:l.value,disabled:!d.value,onClick:U},{default:i(()=>[...e[14]||(e[14]=[u(" 开始导入 ",-1)])]),_:1},8,["icon","loading","disabled"])])]),_:1})),o(me,{modelValue:f.value,"onUpdate:modelValue":e[6]||(e[6]=m=>f.value=m)},null,8,["modelValue"])])}}}),Ze=L(Ee,[["__scopeId","data-v-1d9a1a9f"]]);export{Ze as default};
