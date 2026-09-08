import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
const here=path.dirname(fileURLToPath(import.meta.url));
const moduleRoot=process.env.CODEX_WORKSPACE_NODE_MODULES||path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules');
const sharp=createRequire(path.join(moduleRoot,'package.json'))('sharp');
const manifest=JSON.parse(fs.readFileSync(path.join(here,'manifest.json'),'utf8'));
const errors=[];
for(const f of manifest.items){
 const svg=fs.readFileSync(path.join(here,f.iconKey+'.svg'));
 const {data,info}=await sharp(svg).ensureAlpha().raw().toBuffer({resolveWithObject:true});
 let n=0,minX=96,minY=96,maxX=0,maxY=0;
 for(let y=0;y<info.height;y++)for(let x=0;x<info.width;x++)if(data[(y*info.width+x)*4+3]>5){n++;minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
 if(!n||minX<8||minY<8||maxX>87||maxY>87)errors.push({id:f.id,bounds:[minX,minY,maxX,maxY],pixels:n});
 if(svg.toString().match(/<image|<text|<filter|gradient|vector-effect|NaN|undefined/))errors.push({id:f.id,reason:'禁止元素或非法数值'});
}
const preview=path.join(here,'preview');
for(const f of fs.readdirSync(preview).filter(n=>n.endsWith('.svg')))await sharp(path.join(preview,f)).png().toFile(path.join(preview,f.replace('.svg','.png')));
const duplicateGroups=Object.values(Object.groupBy(manifest.items,f=>f.sha256)).filter(g=>g.length>1).map(g=>g.map(f=>f.id));
const result={revision:2,items:manifest.count,rasterized:manifest.items.length,transparentBoundsErrors:errors,identicalSourceGroups:duplicateGroups,categoryPreviews:12,comparisonAndShowcase:2};
fs.writeFileSync(path.join(here,'validation-results.json'),JSON.stringify(result,null,2)+'\n');
console.log(JSON.stringify(result));if(errors.length)process.exitCode=1;
