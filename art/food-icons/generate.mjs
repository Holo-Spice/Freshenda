import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
import {produce} from './produce.mjs';
import {fruits} from './fruit.mjs';
import {fungi} from './mushrooms.mjs';
import {proteins} from './protein.mjs';
import {seafood} from './seafood.mjs';
import {pantry} from './pantry.mjs';
import {p,line,rect,leaf,group,ink,cream,green} from './drawing.mjs';

const here=path.dirname(fileURLToPath(import.meta.url)),root=path.resolve(here,'../..');
const catalogFile=path.join(root,'docs/鲜序-Codex开发交付包/开发资料/food_catalog.v1.json');
const catalog=JSON.parse(fs.readFileSync(catalogFile,'utf8'));
const xmlDir=path.join(root,'app/src/main/res/drawable'),preview=path.join(here,'preview');
const families=[produce,fruits,fungi,proteins,seafood,pantry];
const moduleRoot=process.env.CODEX_WORKSPACE_NODE_MODULES||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules');
const sharp=createRequire(path.join(moduleRoot,'package.json'))('sharp');
const art=Object.assign({},...families);
if(families.reduce((n,x)=>n+Object.keys(x).length,0)!==Object.keys(art).length)throw Error('重复的绘制定义');
for(const f of catalog.foods)if(!art[f.id])throw Error('缺少逐项设计：'+f.id);
for(const dir of [xmlDir,preview])fs.mkdirSync(dir,{recursive:true});
const esc=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
function svgNodes(nodes){return nodes.map(s=>s.children?`<g transform="translate(${s.x} ${s.y}) rotate(${s.angle}) scale(${s.sx} ${s.sy})">${svgNodes(s.children)}</g>`:`<path d="${s.d}" fill="${s.fill}" stroke="${s.stroke}" stroke-width="${s.width}" stroke-linecap="round" stroke-linejoin="round"/>`).join('');}
function xmlNodes(nodes,indent='  '){return nodes.map(s=>s.children?`${indent}<group android:translateX="${s.x}" android:translateY="${s.y}" android:scaleX="${s.sx}" android:scaleY="${s.sy}" android:rotation="${s.angle}">\n${xmlNodes(s.children,indent+'  ')}\n${indent}</group>`:`${indent}<path android:pathData="${s.d}" android:fillColor="${s.fill==='none'?'#00000000':s.fill}" android:strokeColor="${s.stroke==='none'?'#00000000':s.stroke}" android:strokeWidth="${s.width}" android:strokeLineCap="round" android:strokeLineJoin="round"/>`).join('\n');}
const svg=nodes=>`<?xml version="1.0" encoding="UTF-8"?>\n<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">${svgNodes(nodes)}</svg>\n`;
const xml=nodes=>`<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="96dp" android:height="96dp" android:viewportWidth="96" android:viewportHeight="96">\n${xmlNodes(nodes)}\n</vector>\n`;
const countPaths=nodes=>nodes.reduce((n,s)=>n+(s.children?countPaths(s.children):1),0);
const entries=[],bodies={};
async function normalize(nodes){
 // 以透明像素边界确定留白，SVG 与 Android XML 使用完全相同的外层变换。
 const overscan=`<svg xmlns="http://www.w3.org/2000/svg" width="640" height="640" viewBox="-32 -32 160 160">${svgNodes(nodes)}</svg>`;
 const {data,info}=await sharp(Buffer.from(overscan)).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 let minX=640,minY=640,maxX=-1,maxY=-1;
 for(let y=0;y<info.height;y++)for(let x=0;x<info.width;x++)if(data[(y*info.width+x)*4+3]>5){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
 if(maxX<0||minX===0||minY===0||maxX===639||maxY===639)throw Error('空图或原始绘图越过检测画布');
 const left=minX/4-32,top=minY/4-32,w=(maxX-minX+1)/4,h=(maxY-minY+1)/4;
 const k=+(75/Math.max(w,h)).toFixed(5),tx=+(48-(left+w/2)*k).toFixed(5),ty=+(48-(top+h/2)*k).toFixed(5);
 return [group(nodes,tx,ty,k)];
}
for(const food of catalog.foods){
 const design=art[food.id],nodes=await normalize(design.draw()),source=svg(nodes),android=xml(nodes);
 if(source.includes('NaN')||source.includes('undefined'))throw Error(food.id+' 路径含非法数值');
 fs.writeFileSync(path.join(here,food.iconKey+'.svg'),source);
 fs.writeFileSync(path.join(xmlDir,food.iconKey+'.xml'),android);
 bodies[food.id]=svgNodes(nodes);
 entries.push({id:food.id,name:food.name,categoryId:food.categoryId,iconKey:food.iconKey,iconBrief:food.iconBrief,source:`art/food-icons/${food.iconKey}.svg`,drawable:`app/src/main/res/drawable/${food.iconKey}.xml`,revision:2,family:design.family,designParameters:design.spec,pathCount:countPaths(nodes),status:'AUTHORED_REQUIRES_USER_VISUAL_ACCEPTANCE',sha256:crypto.createHash('sha256').update(source).digest('hex')});
}
const special={ic_food_custom:[rect(18,22,60,58,10,cream),leaf(48,64,30,32,30,green),line('M 48 43 V 69 M 36 56 H 60',ink,2.5)],ic_stat_freshness:[p('M 24 19 Q 24 12 31 12 H 65 Q 72 12 72 19 V 77 Q 72 84 65 84 H 31 Q 24 84 24 77 Z','#FFFFFF','none'),line('M 24 39 H 72 M 34 23 V 32 M 34 49 V 67','#00000000',0)]};
// 通知小图标只用白色 alpha 轮廓；内部结构使用透明留白路径。
special.ic_stat_freshness=[p('M 25 14 H 71 V 82 H 25 Z M 31 20 V 35 H 65 V 20 Z M 31 41 V 76 H 65 V 41 Z','#FFFFFF','none'),p('M 35 24 H 39 V 31 H 35 Z M 35 47 H 39 V 61 H 35 Z','#FFFFFF','none')];
for(const[key,nodes]of Object.entries(special)){fs.writeFileSync(path.join(here,key+'.svg'),svg(nodes));fs.writeFileSync(path.join(xmlDir,key+'.xml'),xml(nodes));}
const header=(w,h)=>`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><rect width="${w}" height="${h}" fill="#E8E5D9"/>`;
const label=(x,y,text,size=15)=>`<text x="${x}" y="${y}" text-anchor="middle" font-family="Microsoft YaHei, Noto Sans CJK SC, sans-serif" font-size="${size}" fill="#203F36">${esc(text)}</text>`;
const inline=(id,x,y,size)=>`<svg x="${x}" y="${y}" width="${size}" height="${size}" viewBox="0 0 96 96">${bodies[id]}</svg>`;
for(const cat of catalog.categories){
 const items=entries.filter(e=>e.categoryId===cat.id),cols=4,cw=224,ch=172,w=cols*cw,h=Math.ceil(items.length/cols)*ch+76;
 let sheet=header(w,h)+label(w/2,35,`${cat.name} · ${items.length} 枚 · 第二版`,23);
 items.forEach((f,i)=>{const x=i%cols*cw,y=Math.floor(i/cols)*ch+66;sheet+=`<rect x="${x+6}" y="${y}" width="${cw-12}" height="${ch-10}" rx="12" fill="#F5EFDC"/>`+inline(f.id,x+50,y+5,124)+label(x+cw/2,y+140,f.name)+label(x+cw/2,y+155,f.iconKey,9);});
 fs.writeFileSync(path.join(preview,cat.id+'.svg'),sheet+'</svg>');
}
const showcase=['broccoli','carrot','tomato','potato','beef_steak','chicken_drumstick','salmon','shrimp','milk','yogurt','egg','cheddar'];
let hero=header(960,750)+label(480,38,'鲜序 · 食材图标重绘',25)+label(480,65,'实物轮廓 · 局部填色 · 内部纹理 · 可编辑矢量',14);
showcase.forEach((id,i)=>{const f=entries.find(e=>e.id===id),x=i%4*240,y=90+Math.floor(i/4)*218;hero+=`<rect x="${x+10}" y="${y}" width="220" height="202" rx="16" fill="#F5EFDC"/>`+inline(id,x+28,y+5,150)+inline(id,x+173,y+80,48)+label(x+120,y+177,f.name,17)+label(x+194,y+146,'48 px',9);});
fs.writeFileSync(path.join(preview,'showcase.svg'),hero+'</svg>');
// 旧版/新版对应图用于直接比较造型，而不以哈希不同代替辨识度评审。
const compare=['broccoli','carrot','shiitake','enoki','king_oyster','kiwi','avocado','pork_belly','milk','yogurt','salmon','live_mussel'];
let before=header(960,990)+label(480,36,'上一版 / 重绘版',24);
compare.forEach((id,i)=>{const f=entries.find(e=>e.id===id),x=i%4*240,y=64+Math.floor(i/4)*304;const oldFile=path.join(root,'art/food-icons-backup-v1/source',f.iconKey+'.svg');const old=fs.readFileSync(oldFile,'utf8').replace(/^[\s\S]*?<svg[^>]*>/,'').replace(/<\/svg>\s*$/,'');before+=`<rect x="${x+7}" y="${y}" width="226" height="289" rx="12" fill="#F5EFDC"/><svg x="${x+73}" y="${y+8}" width="96" height="96" viewBox="0 0 96 96">${old}</svg>`+label(x+120,y+116,'上一版',11)+inline(id,x+48,y+121,145)+label(x+120,y+279,f.name,16);});
fs.writeFileSync(path.join(preview,'comparison.svg'),before+'</svg>');
const cards=entries.map(f=>`<article data-category="${f.categoryId}" data-search="${esc(f.name+' '+f.id+' '+f.iconKey)}"><div class="picture"><svg viewBox="0 0 96 96" role="img" aria-label="${esc(f.name)}">${bodies[f.id]}</svg></div><strong>${esc(f.name)}</strong><small>${f.iconKey}</small><p>${esc(f.iconBrief)}</p></article>`).join('');
const html=`<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>鲜序 · 食材图标第二版</title><style>*{box-sizing:border-box}body{margin:0;background:#E8E5D9;color:#203F36;font:15px 'Microsoft YaHei',system-ui}header{padding:30px max(24px,calc((100vw - 1260px)/2));background:#203F36;color:#F5EFDC}h1{margin:0 0 10px;font-size:28px}header p{margin:0;color:#CFD7BF}.toolbar{position:sticky;top:0;z-index:2;background:#E8E5D9ee;backdrop-filter:blur(8px);padding:15px 24px;display:flex;gap:12px;flex-wrap:wrap;align-items:center;border-bottom:1px solid #A9B49F}.toolbar input[type=search],select{padding:10px;border:1px solid #A3B197;border-radius:8px;background:#F5EFDC;color:#203F36}main{max-width:1310px;margin:24px auto;padding:0 24px}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(195px,1fr));gap:14px}article{text-align:center;background:#F5EFDC;border:1px solid #CDD3BF;border-radius:14px;padding:12px}.picture{height:152px;display:grid;place-items:center}.picture svg{width:var(--size,132px);height:var(--size,132px)}article strong{display:block;font-size:16px}article small{font-size:10px;overflow-wrap:anywhere;color:#697965}article p{font-size:12px;line-height:1.5;color:#697965;margin:7px 0 0}.hide-labels strong,.hide-labels small,.hide-labels article p{visibility:hidden}a{color:inherit}.count{margin-left:auto}</style><header><h1>鲜序 · 食材图标第二版</h1><p>322 枚独立食材配置 / SVG 与 Android 矢量同步 / <a href="showcase.png">代表图标</a> / <a href="comparison.png">新旧对照</a></p></header><div class="toolbar"><input id="search" type="search" placeholder="搜索名称或 iconKey"><select id="category"><option value="">全部分类</option>${catalog.categories.map(c=>`<option value="${c.id}">${c.name}</option>`).join('')}</select><label>尺寸 <select id="size"><option value="48">48 px</option><option value="64">64 px</option><option value="96">96 px</option><option value="132" selected>132 px</option></select></label><label><input id="labels" type="checkbox">隐藏名称检查辨识度</label><span class="count" id="count">322 枚</span></div><main><div class="grid">${cards}</div></main><script>const cards=[...document.querySelectorAll('article')];function filter(){const q=document.querySelector('#search').value.toLowerCase(),c=document.querySelector('#category').value;let n=0;cards.forEach(a=>{const ok=(!c||a.dataset.category===c)&&a.dataset.search.toLowerCase().includes(q);a.hidden=!ok;if(ok)n++;});document.querySelector('#count').textContent=n+' 枚';}document.querySelector('#search').addEventListener('input',filter);document.querySelector('#category').addEventListener('change',filter);document.querySelector('#size').addEventListener('change',e=>document.documentElement.style.setProperty('--size',e.target.value+'px'));document.querySelector('#labels').addEventListener('change',e=>document.body.classList.toggle('hide-labels',e.target.checked));</script></html>`;
fs.writeFileSync(path.join(preview,'index.html'),html);
fs.writeFileSync(path.join(here,'manifest.json'),JSON.stringify({schemaVersion:2,revision:2,catalogDataVersion:catalog.dataVersion,style:{viewBox:'0 0 96 96',outline:ink,background:'transparent',method:'Explicit per-food vector designs; semantic shape families; no random or hash-based decorations'},count:entries.length,items:entries},null,2)+'\n');
console.log(JSON.stringify({foodSVG:entries.length,foodVector:entries.length,previewSheets:catalog.categories.length+2,totalPaths:entries.reduce((n,e)=>n+e.pathCount,0),uniqueSourceHashes:new Set(entries.map(e=>e.sha256)).size}));
